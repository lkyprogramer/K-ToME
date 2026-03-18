> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-06-minimal-official-slice.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-07 Short Run Expansion

**阶段**: `Phase 2 / P2-W7`  
**优先级**: `P1`  
**前置条件**: `P2-W6` 完成  
**对应问题**: 最小正式切片只证明“路径成立”，还不足以证明 Phase 2 出口。必须扩成 4 职业、4 zone 和完整短局实验室，Phase 2 才能真正结束。

---

## 1. 阶段目标

把最小正式切片扩成 `4 职业 + 4 zone + 24 怪 + 24 物品 + SoloClearLab v1` 的完整短局。

完成标准：

1. `Rogue`、`Templar` 进入正式路径。
2. 扩成 `4` 个 zone。
3. 内容达到 `24` 怪、`24` 物品最低标准。
4. `SoloClearLab v1` 成为 Phase 2 出口实验室。
5. 4 职业、4 zone、24 怪、24 物品对应的图像/音频/manifest/key 全部形成完整短局矩阵。

## 2. 当前问题

1. 只有 2 职业切片无法证明 Phase 2 的合同足以承载多职业短局。
2. 没有实验室入口时，短局是否稳定只能靠人工印象。
3. 如果 4 zone、4 职业、24 怪、24 物品没有按矩阵化方式创建，Phase 2 后期很容易继续用临时补丁堆量。

### 2.1 本 PR 必须冻结的口径

1. Phase 2 最终只承诺 4 基础职业，不提前引入进阶职业。
2. 只做短局，不做长局世界分支。
3. `SoloClearLab v1` 必须成为正式门禁，不只是脚本草案。
4. 每个职业至少 8 个正式 talent、每个 zone 至少 1 套完整 tileset family、每个 zone 至少 1 组 ambience/cue。
5. `Rogue` 在 Phase 2 直接使用 `ENERGY` 主资源，不再写成 `STAMINA + MOMENTUM reserve` 的过渡方案。

## 3. 范围与非目标

### 3.1 范围

1. `Rogue`
2. `Templar`
3. 4 zone 短局
4. 24 怪 + 24 物品
5. `SoloClearLab v1`
6. Phase 2 出口收口
7. 4 zone 对应的资源与音频扩展

### 3.2 非目标

1. 不在本 PR 实现长局。
2. 不在本 PR 上线进阶职业。
3. 不在本 PR 深化 ProcGen 与 Loot 预算。
4. 不在本 PR 做 Phase 3 的 telegraph 深度或 Boss phase 多段语义。

## 4. 技术方案

### 4.1 职业扩展

新增：

1. `Rogue`
2. `Templar`

冻结口径：

1. 重点验证 `ENERGY`、`POSITIVE_ENERGY` 等第二批资源语义。
2. 职业都必须具备最小通关 build，而不是先追求大量支线。

职业最小正式包：

以下 `talent id` 同样直接对齐核心设计文档 `5.2.2` 的长期树定义；Phase 2 只裁剪冻结子集，不再扩出额外旁支。

#### Rogue

1. 主资源：`ENERGY`
2. 回复策略：`Composite([PerTurn(5), OnHit(8)])`
3. 最小 talent 包：
   - `backstab`（背刺）
   - `poison_blade`（毒刃）
   - `stealth`（隐匿）
   - `smoke_bomb`（烟雾弹）
   - `roll`（翻滚）
   - `blade_flurry`（刀刃乱舞）
   - `shadowstep`（暗影步）
   - `deathblow`（致命一击）
4. 视觉/音频 key：
   - `actor.rogue`
   - `portrait.rogue`
   - `audio.profession.rogue`
5. 最小 build contract：
   - 机动清杂
   - 单体爆发
   - 最低限度的脱战/逃生 answer
6. `shadowstep` 虽然是 Subtlety 树 `T4`，但它承担 Rogue 的关键 Mobility Answer；若不前置，Phase 2 的 Solo-Clear Contract 会缺口。

#### Templar

1. 主资源：`POSITIVE_ENERGY`
2. 回复策略：`Composite([OnDamageTaken(0.15), OnHit(3), DecayPerTurn(5)])`
3. 最小 talent 包：
   - `holy_strike`（神圣打击）
   - `judgment_hammer`（审判之锤）
   - `holy_light`（圣光）
   - `holy_shield`（圣盾）
   - `devotion`（虔诚）
   - `holy_aura`（神圣光环）
   - `purify`（净化）
   - `divine_intervention`（神圣庇护）
4. 视觉/音频 key：
   - `actor.templar`
   - `portrait.templar`
   - `audio.profession.templar`
5. 最小 build contract：
   - `HOLY` 主输出
   - 净化/护盾
   - 对亡灵/恶魔的 Boss answer
6. Grace 树在 Phase 2 有意完整选入 `holy_light / holy_shield / purify / divine_intervention`：其中 `divine_intervention` 是 T4，但承担唯一稳定的 Panic Answer。Phase 3 的成长空间默认放在 Smite / Faith 树，而不是继续扩 Grace 树。

Phase 2 最终 4 职业 talent 目标：

1. `Vanguard`：8
2. `Arcanist`：8
3. `Rogue`：8
4. `Templar`：8

### 4.2 4 Zone 短局结构

建议最小结构：

1. 初始 zone
2. 野外/边缘 zone
3. 工坊/矿坑 zone
4. 最终 boss zone

冻结口径：

1. 4 zone 以短局节奏闭环为主。
2. 不引入复杂分支世界图。

Phase 2 四个 zone 最低规格：

| zone | 推荐等级 | 主要敌群 | 主要通道 | 主要任务物件 |
| --- | --- | --- | --- | --- |
| `shattered_outpost` | 1~4 | 鼠群、盗匪、骷髅 | `PHYSICAL / SHADOW` | 武库门、补给箱、警报篝火 |
| `greenwood_fringe` | 3~6 | 狼、巡林守卫、弓手 | `PHYSICAL / COLD / LIGHTNING` | 狩猎印记、路障、祭树 |
| `deep_iron_pit` | 5~8 | 兽人矿工、铸炉守卫、元素工匠 | `PHYSICAL / FIRE` | 升降机、熔炉、钥匙架 |
| `grey_gate_depths` | 7~10 | 暗影祭司、亡骸、Boss | `SHADOW / HOLY` | 封印门、祭坛、Boss arena |

四个 zone 的最低规格也必须一并冻结：

| zoneId | floorCount | mapSize | 环境主题 | 特殊机制 |
| --- | --- | --- | --- | --- |
| `shattered_outpost` | `2` | `60 x 40` | 废墟 / 石材 / 杂草 | 教程区、简单陷阱 |
| `greenwood_fringe` | `2` | `70 x 45` | 森林 / 小溪 / 苔藓 | 视野遮挡、巡逻怪 |
| `deep_iron_pit` | `2` | `80 x 50` | 矿洞 / 铁轨 / 熔炉 | 熔岩地形、矿车机关 |
| `grey_gate_depths` | `2` | `80 x 50` | 古城地下 / 石柱 / 暗河 | 最终 Boss、封印门 |

### 4.3 Monster 与 Item 配额矩阵

建议 24 怪最小拆分：

1. `shattered_outpost`
   - 6 个
2. `greenwood_fringe`
   - 6 个
3. `deep_iron_pit`
   - 6 个
4. `grey_gate_depths`
   - 6 个

结构预算进一步固定为：

1. `18` 个普通怪
2. `4` 个精英
3. `2` 个 Boss
4. 所有精英/Boss 至少使用 Layer 2 simple scripted AI

每个 zone 至少包含：

1. 2 个 normal melee
2. 1 个 ranged 或 artillery
3. 1 个 controller/support
4. 1 个 elite 候选
5. 1 个 zone signature 敌人

建议 24 物品最小拆分：

1. 武器：6
2. 防具：6
3. 饰品：4
4. 消耗品：6
5. quest/boss/reward item：2

### 4.4 Phase 2 资源与音频扩展基线

Phase 2 最低资源 DoD 必须在本 PR 收口：

1. 4 zone 的基础 Tile family
2. 4 基础职业的主体 sprite 或 portrait
3. 核心 skill/status/item/task icon
4. UI 核心 cue、脚步、近战、施法、Boss 预警、基础 ambience

建议资产族清单：

1. zone tileset:
   - `tileset.ruins`
   - `tileset.forest_edge`
   - `tileset.mine`
   - `tileset.shadow_depths`
2. actor:
   - `actor.vanguard`
   - `actor.arcanist`
   - `actor.rogue`
   - `actor.templar`
3. UI / icon:
   - `icon.skill.*`
   - `icon.status.*`
   - `icon.item.*`
   - `icon.quest.*`
4. audio cue:
   - `ui.*`
   - `footstep.*`
   - `melee.*`
   - `spell.*`
   - `monster.*`
   - `interactable.*`
   - `ambience.*`
   - `music.phase2_*`

Phase 2 图片生成入口约束：

1. Phase 2 正式图片统一通过 `scripts/generate_assets.sh` 调用 Gemini 生成
2. 资产计划统一收敛到 `assets-src/image/specs/phase2-asset-plan.yaml`
3. 没有 `GEMINI_API_KEY` 时不得开始正式图片生成

### 4.5 SoloClearLab v1

实验室必须固定：

1. seed
2. 职业
3. zone route
4. 胜负口径
5. 关键场景：
   - 杂兵包
   - 精英战
   - Boss 战

`SoloClearLab v1` 从本 PR 起升级为硬门禁，固定如下：

| 场景 | 配置 | 标定等级 | 装备预算 | 通过标准 |
| --- | --- | --- | --- | --- |
| 杂兵包 | `10 x 10` 封闭房间，`6` 只普通怪 | `5` | 阶段蓝装全套 | 清光敌人且 `HP > 30%` |
| 精英战 | `15 x 15` 房间，`1` 精英 + `2` 普通怪 | `7` | 阶段蓝装全套 | 击杀精英且角色存活 |
| Boss 战 | `20 x 20` Boss 房 | `10` | 阶段蓝装全套 | 击杀 Boss |

固定黄金 seed：

1. `20260313`
2. `20260314`
3. `20260315`

四职业最低覆盖矩阵：

1. `Vanguard`
   - 抗压杂兵包
   - 盾姿态精英战
   - 物理 Boss answer
2. `Arcanist`
   - AOE 杂兵包
   - 护盾/位移精英战
   - 法术资源 Boss answer
3. `Rogue`
   - 机动清杂
   - 潜伏/爆发精英战
   - 低容错 Boss answer
4. `Templar`
   - 圣光控场
   - 净化/护盾精英战
   - `HOLY/SHADOW` Boss answer

## 5. 推荐改动面

### 5.1 `game`

1. `Rogue / Templar`
2. 额外 3 个 zone
3. 怪物与物品扩展到出口标准
4. 四职业 talent 包收口

### 5.2 `tools`

1. `SoloClearLab v1`
2. Phase 2 出口聚合脚本

### 5.3 `client`

1. 4 zone tileset 与 ambience 接线
2. 4 职业主体资源接线

## 6. 测试与自证

### 6.1 必测类

1. `ShortRunRosterTest`
2. `ZoneChainSmokeTest`
3. `SoloClearLabTest`
4. `Phase2ContentCoverageTest`
5. `Phase2AssetCoverageTest`
6. `Phase2HeadlessMatrixSmokeTest`
7. `Phase2ClientSmokeTest`

### 6.2 必测行为

1. 4 职业都能进入正式短局。
2. 4 zone 流程稳定可结束。
3. `24 怪 + 24 物品` 数据完整且可进局。
4. `SoloClearLab v1` 能稳定覆盖四职业的关键场景。
5. 4 zone、4 职业、核心 icon/cue/tileset 资源都能在 manifest 中解析。
6. `headlessSmoke` 必须扩成四职业 / 多 zone 的最小矩阵，而不是继续停留在单职业 smoke。
7. `clientSmoke` 必须覆盖默认正式短局主路径。
8. `preReleaseAcceptance`、`longRunLab`、`soloClearLab` 三者都必须保持绿色。
9. PR 新增职业、zone、怪物、物品、资源系统必须进入 smoke 或 lab，不允许只出现在 schema/data test 中。

### 6.3 自动化命令

```bash
./gradlew :game:test
./gradlew soloClearLab
./gradlew localeLint
./gradlew contractLint
./gradlew assetLint
./gradlew audioLint
./gradlew goldenScreenshot
./gradlew headlessSmoke
./gradlew clientSmoke
./gradlew longRunLab
./gradlew preReleaseAcceptance
./gradlew test
```

当前离线资源自检入口：

```bash
python3 scripts/asset-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/style-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/manifest-lint.py \
  --plan assets-src/image/specs/phase2-asset-plan.yaml \
  --manifest assets-src/image/manifests/phase2-visual-manifest.json
```

### 6.4 白盒验证

1. 用四个职业各完成一局默认短局。
2. 检查：
   - 4 zone 路线闭环成立
   - 杂兵包、精英战、Boss 战都能覆盖
   - locale、Tile、日志、资源路径稳定
   - 4 职业主体资源、4 zone ambience、核心 icon/cue 都真实生效
3. 对照 `headlessSmoke`、`clientSmoke`、`SoloClearLab` 报告，确认默认正式短局与桌面白盒一致。

## 7. 出口门禁

1. `4 职业 + 4 zone + 24 怪 + 24 物品` 达标。
2. `SoloClearLab v1` 全绿。
3. `locale-lint`、`contract-lint`、`golden screenshot`、`save/load` 全绿。
4. Phase 2 正式玩家路径不再依赖 ASCII。
5. Phase 2 资源最低 DoD 达标，后续进入 Phase 3 时不再补回“最基础的职业/zone/资源骨架”。
6. `style-lint`、`asset-lint`、`audio-lint`、`manifest-lint` 一并成为 Phase 2 出口门禁。
7. `P2-B/P2-C` 资产计划最小集合冻结，职业、地图、怪物、UI 图标基础位不允许在实现期临时改名或缺项。
8. `headlessSmoke`、`clientSmoke`、`soloClearLab`、`preReleaseAcceptance` 已成为 Phase 2 正式内容出口门禁。
9. Phase 2 新增内容必须补进 smoke、lab 或 golden 主路径，不允许只留 isolated unit test。

## 8. 风险与止损

1. 如果内容量不足以支撑四职业实验室，优先补实验室必需内容，不优先堆可选资源。
2. 如果 `SoloClearLab` 胜负口径漂移，必须先冻结实验室契约。
3. 如果某职业只有“理论可玩”但无法稳定通关，Phase 2 不能结束。

## 9. 当前状态

1. 本文是 `P2-W7` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 才能真正达到出口标准。

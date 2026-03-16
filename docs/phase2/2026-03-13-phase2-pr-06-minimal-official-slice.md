> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-05-minimal-tile-shell.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-06 Minimal Official Slice

**阶段**: `Phase 2 / P2-W6`  
**优先级**: `P1`  
**前置条件**: `P2-W5` 完成  
**对应问题**: 只有合同和 UI 壳层还不够，必须尽快建立一条最小正式内容切片，才能验证 `Tile + i18n + manifest + schema + core contracts` 是否真的能协同工作。

---

## 1. 阶段目标

建立 `2 职业 + 1 zone 链 + 1 Boss + 最小资源集` 的正式内容切片。

完成标准：

1. `Vanguard` 与 `Arcanist` 能进入正式 Tile 路径并完成短切片。
2. 至少 `1` 条 zone 链和 `1` 个 Boss 可通关。
3. 最小美术、音频、图标资源集进入主路径。
4. 30~60 分钟切片闭环成立。
5. 职业、技能、怪物、地图、交互物和资源 key 的第一批正式对象全部真实创建，而不是只留 schema 空壳。

## 2. 当前问题

1. 目前只有系统骨架，没有真实内容切片就无法验证玩家体验。
2. 如果直接跳到 4 职业 4 zone，会放大缺口定位成本。
3. 如果最小切片不把职业树、怪物 archetype、zone 交互物和资源集一起建出来，后面短局扩展仍会继续漂。

### 2.1 本 PR 必须冻结的口径

1. 先只做 2 职业。
2. 先只做 1 条短切片 zone 链。
3. Boss 只要求足以验证 Phase 2 路径，不要求 Phase 3 深度 telegraph。
4. 这两个职业和这条 zone 链所需的技能、怪物、交互物、美术和音频都必须在本 PR 同步进入正式路径。

## 3. 范围与非目标

### 3.1 范围

1. `Vanguard`
2. `Arcanist`
3. 1 条 zone 链
4. 1 Boss
5. 最小视觉/音频资源集
6. 该切片所需的 monster / item / interactable 最小集合

### 3.2 非目标

1. 不在本 PR 完成 4 职业全覆盖。
2. 不在本 PR 建立完整长局结构。
3. 不在本 PR 追求高内容量。
4. 不在本 PR 提前引入 Rogue/Templar 的可玩路径，但它们的 schema 和 key 已在前置 PR 冻结。

## 4. 技术方案

### 4.1 职业选择

先上：

1. `Vanguard`
2. `Arcanist`

原因：

1. 一个物理职业，一个法术职业，足以验证基础资源差异。
2. 便于先验证 `PHYSICAL / FIRE / COLD / MANA / STAMINA` 主线。

职业最小正式包必须包括：

以下 `talent id` 直接对齐核心设计文档 `5.2.2` 的长期树定义；Phase 2 只是从长期树里裁剪冻结子集，不再额外发明平行天赋名。

#### Vanguard

1. 主资源：`STAMINA`
2. 回复策略：`PerTurn(3)`
3. 最小 talent 包：
   - `power_strike`（猛力打击）
   - `sweeping_strike`（横扫）
   - `shield_bash`（盾击）
   - `guard_stance`（格挡姿态）
   - `war_cry`（战吼）
   - `intimidation`（威压）
   - `sunder_armor`（碎甲）
   - `unyielding`（不屈）
4. 起始套装：
   - 单手武器
   - 基础盾
   - 中甲
   - 恢复药剂
5. 视觉/音频 key 最低要求：
   - `actor.vanguard`
   - `portrait.vanguard`
   - `audio.profession.vanguard`
6. 最小 build contract：
   - 1 条主输出树
   - 1 条防御/控场树
   - 1 条 Panic Answer（`unyielding` 或等价）
7. `unyielding` 虽然是 Warcry 树 `T4`，但它是 Vanguard 在 Phase 2 最小切片里唯一稳定的 Panic Answer，因此按 Solo-Clear Contract 例外前置到本阶段。

#### Arcanist

1. 主资源：`MANA`
2. 回复策略：`PerTurn(2)`
3. 最小 talent 包：
   - `fireball`（火球）
   - `flame_wall`（烈焰之墙）
   - `ice_bolt`（冰箭）
   - `frost_nova`（霜冻新星）
   - `arcane_shield`（奥术护盾）
   - `blink`（闪现）
   - `mana_surge`（法力涌动）
   - `ice_prison`（冰封）
4. 起始套装：
   - 法杖或法器
   - 轻袍
   - 法力药剂
5. 视觉/音频 key 最低要求：
   - `actor.arcanist`
   - `portrait.arcanist`
   - `audio.profession.arcanist`
6. 最小 build contract：
   - 1 条火系或冰系主输出树
   - 1 条防护/位移树
   - 1 条 Boss answer（`arcane_shield` / `blink` / 等价组合）

### 4.2 Zone 与 Boss 切片

建议：

1. `Shattered Outpost` 或等价初始 zone
2. 最终接 `1` 个短切片 Boss arena

冻结口径：

1. zone 目标是验证切片闭环，不是铺量。
2. Boss 必须能触发基础资源、状态、日志、掉落、结算。
3. 首个 elite/Boss 至少使用 Layer 2 simple scripted AI，不允许仍然完全依赖 `CHASE`。

推荐首批切片：

1. zone:
   - `shattered_outpost`
2. 交互物：
   - `armory_gate`
   - `supply_crate`
   - `alarm_bonfire`
3. monster pool:
   - `beast.rat_scavenger`
   - `goblin.scout`
   - `bandit.raider`
   - `bandit.archer`
   - `undead.restless_skeleton`
   - `undead.bone_guard`
4. elite / boss:
   - `bandit.captain`

`shattered_outpost` 切片规格在本 PR 必须固定为：

| 字段 | 口径 |
| --- | --- |
| floorCount | `2` |
| mapSize | `60 x 40` |
| recommendedLevel | `1~4` |
| environmentTheme | 废墟 / 石材 / 杂草 |
| specialMechanics | 教程区、简单陷阱、少量巡逻压力 |
| normalPool | 鼠、哥布林、骷髅、强盗 |
| elitePool | 强盗头目 |

### 4.3 地图、人物与交互物初始化基线

本 PR 必须同时建立以下正式对象：

1. `ZoneSpec`:
   - `tilesetKey`
   - `ambientProfile`
   - `monsterPools`
   - `elitePools`
   - `objectiveSetId`
2. `MonsterTemplateV2`:
   - `archetype`
   - `visualKey`
   - `audioProfile`
   - `aiProfileId`
   - `lootProfileId`
3. `ProfessionDef`:
   - `startingResources`
   - `startingKit`
   - `talentTrees`
   - `soloContract`
4. `TalentDef`:
   - `iconKey`
   - `visualKey`
   - `audioProfile`
   - `resourceCosts`
   - `targeting`
5. interactable:
   - `visualKey`
   - `audioProfile`
   - `interactionTags`

### 4.4 最小资源集

必须至少具备：

1. terrain tiles
2. actor tiles
3. item icons
4. boss / elite 标识资源
5. UI confirm / hit / spell / ambience 基础音频

最小图像交付：

1. `tileset.ruins` 全套基础 Tile
2. `actor.vanguard`
3. `actor.arcanist`
4. `actor.bandit.*`
5. `actor.undead.*`
6. `icon.skill.vanguard.*`
7. `icon.skill.arcanist.*`
8. `icon.status.*`
9. `icon.item.*`

最小音频交付：

1. `ui.confirm`
2. `footstep.stone`
3. `melee.hit_light`
4. `spell.fire_basic`
5. `spell.arcane_blink`
6. `monster.bandit_alert`
7. `ambience.ruins_wind`
8. `boss.warning`

切片资源验收约束：

1. 每个正式图像都必须有 `AssetSpec`
2. 每个正式音频都必须有 `AudioSpec`
3. 每个正式图片都必须记录 style tag、来源和人工挑选/清理信息
4. 每个正式资源都必须完成 manifest 映射后才能进入主线
5. P2-B 相关图片资产必须通过 `assets-src/image/specs/phase2-asset-plan.yaml` 的 `P2-B` gate 管理

### 4.5 切片流程与奖励基线

推荐最小流程：

1. 出生点
2. 初始杂兵区
3. 中段交互物目标
4. elite 或 mini-arena
5. boss arena
6. 结算与掉落展示

掉落最低要求：

1. 每职业至少 1 件早期核心武器
2. 至少 2 种基础防具
3. 至少 3 种消耗品
4. 至少 1 件 boss 奖励物

## 5. 推荐改动面

### 5.1 `game`

1. 2 职业内容
2. zone 链
3. boss encounter
4. monster / interactable / loot profile 最小集

### 5.2 `client`

1. 资源接线
2. 结算与流程 UI
3. slice 内 boss / interactable 的视觉与音频接线

## 6. 测试与自证

### 6.1 必测类

1. `OfficialSliceDataTest`
2. `BossEncounterSmokeTest`
3. `SliceGoldenFlowTest`
4. `ProfessionStarterKitTest`
5. `ZoneSpecSliceTest`

### 6.2 必测行为

1. 2 职业都能进入同一切片。
2. 切片从开局到 Boss/结算成立。
3. 掉落、日志、资源、状态都能在正式路径显示。
4. 交互物、怪物 archetype、职业 starter kit 都能被 loader 和 runtime 正确消费。

### 6.3 自动化命令

```bash
./gradlew :game:test
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew goldenScreenshot
./gradlew contractLint
```

当前离线资源自检入口：

```bash
python3 scripts/asset-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/style-lint.py --plan assets-src/image/specs/phase2-asset-plan.yaml
python3 scripts/manifest-lint.py \
  --plan assets-src/image/specs/phase2-asset-plan.yaml \
  --manifest assets-src/image/manifests/phase2-visual-manifest.json
```

图片生成入口：

```bash
GEMINI_API_KEY=your_key ./scripts/generate_assets.sh \
  assets-src/image/specs/phase2-asset-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase2-generation-report.jsonl
```

### 6.4 白盒验证

1. 用 `Vanguard` 跑完整切片。
2. 用 `Arcanist` 跑完整切片。
3. 检查：
   - 开局 -> 探索 -> Boss -> 结算闭环
   - Tile、音频、日志、掉落都在正式路径
   - 交互物、Boss 警报、职业技能图标和职业主体资源都真实生效

## 7. 出口门禁

1. `2 职业 + 1 zone 链 + 1 Boss` 正式切片可通关。
2. 最小资源集进入主路径。
3. 30~60 分钟切片稳定。
4. 两职业的 starter talent、starter kit、职业主体资源、monster/zone/interactable 基线全部真实落地。
5. 切片内正式资源全部通过 `asset/style/audio/manifest` lint。
6. `P2-B` 资产计划、视觉 manifest 与 Gemini 生成入口三者保持一一对应，不允许手填漂移。

## 8. 风险与止损

1. 如果切片时长膨胀，优先缩短 zone 链，不继续堆内容。
2. 如果资源不足，优先保证识别度和流程闭环，不优先补美术量。
3. 如果 Boss 设计开始向 Phase 3 深度漂移，必须收回到 Phase 2 验证定位。

## 9. 当前状态

1. 本文是 `P2-W6` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 会首次拥有正式内容切片闭环。

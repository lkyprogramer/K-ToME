> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-02-core-semantic-contracts.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-03 Locale & Schema V2

**阶段**: `Phase 2 / P2-W3`  
**优先级**: `P0`  
**前置条件**: `P2-W1`、`P2-W2` 完成  
**对应问题**: 现有内容仍大量依赖 `name/glyph/color` 等 Phase 1 字段，正式内容对象还没有统一 schema 版本与 i18n key。继续堆内容会直接放大后续返工。

---

## 1. 阶段目标

完成 `Schema V2 + i18n key + 首页 locale 选择 + lint` 的正式化。

完成标准：

1. 正式内容对象切换到 `nameKey/descKey/visualKey/iconKey/audioProfile/schemaVersion`。
2. 首页支持 locale 选择，局内文本按当前 locale 渲染。
3. `locale-lint` 与 `contract-lint` 建立并接入回归入口。
4. 正式内容不再新增裸中文或裸英文。
5. `profession / talent / monster / boss / zone / item` 的正式 schema 和 key 命名基线在本 PR 固定。

## 2. 当前问题

1. 文本与展示字段混在 schema 里，难以长期扩展。
2. 没有 key 化时，日志和 UI 文本无法跨语言重渲染。
3. 没有 lint 时，缺 key、错 key、漏资源会在运行时才暴露。
4. 目前还没有给职业、技能、怪物、地图这些正式对象建立统一 schema 与目录布局，后续内容创建极易跑偏。

### 2.1 本 PR 必须冻结的口径

1. `name/desc` 正式路径全部转 key。
2. `schemaVersion` 是内容对象强制字段。
3. `locale-lint` 与 `contract-lint` 是门禁，不是可选工具。
4. `visualKey/iconKey/audioProfile` 必须在对象创建的第一天就跟着 schema 走，不能后补。

## 3. 范围与非目标

### 3.1 范围

1. 内容 schema V2
2. i18n 资源文件
3. 首页 locale 选择与局内应用
4. lint 工具与 Gradle 入口
5. 正式内容目录布局与命名空间

### 3.2 非目标

1. 不在本 PR 完成完整 Tile 表现。
2. 不在本 PR 处理全部资源生产。
3. 不在本 PR 追求全量翻译 polish。
4. 不在本 PR 完成所有 Phase 2 内容量，但必须把首批正式对象 skeleton 先建出来。

## 4. 技术方案

### 4.1 Schema V2 切换

建议文件：

```text
game/src/main/resources/data/**/*.yaml
game/src/main/kotlin/com/ktome/game/data/schema/*
game/src/test/kotlin/com/ktome/game/data/*
```

冻结口径：

1. 正式对象至少具备：
   - `id`
   - `nameKey`
   - `descKey`
   - `visualKey`
   - `iconKey`
   - `audioProfile`
   - `schemaVersion`
   - `tags`
2. `glyph/color` 可以保留为 debug/fallback，但不再是正式语义真源。

必须覆盖的正式对象族：

1. `profession`
2. `talent`
3. `monster`
4. `bossEncounter`
5. `zone`
6. `item`
7. `lootProfile`
8. `tileset`
9. `difficulty`

### 4.2 内容目录与 Registry 布局

建议目录：

```text
game/src/main/resources/data/
  professions/
  talents/
  monsters/
  bosses/
  zones/
  items/
  loot/
  tilesets/
  difficulties/
game/src/main/resources/i18n/
  zh-CN.json
  en-US.json
```

冻结口径：

1. 一个对象族一个目录，不混在“巨大 yaml 大杂烩”里。
2. 同一对象的规则字段、显示 key、资源 key 必须在同一 schema 中出现，不允许跨 2~3 份手写表拼接。
3. `profession`、`talent`、`zone`、`monster`、`bossEncounter` 的 cross-reference 必须都能被 schema 和 lint 校验。

### 4.3 Phase 2 首批正式对象创建基线

本 PR 必须先把以下对象 skeleton 建出来，哪怕部分内容在后续 PR 才补完：

1. profession:
   - `vanguard`
   - `arcanist`
   - `rogue`
   - `templar`
2. zone:
   - `shattered_outpost`
   - `greenwood_fringe`
   - `deep_iron_pit`
   - `grey_gate_depths`
3. difficulty:
   - `normal`
4. starter talent tree namespace:
   - `vanguard_arms`
   - `vanguard_shield`
   - `vanguard_warcry`
   - `arcanist_flame`
   - `arcanist_frost`
   - `arcanist_arcane`
   - `rogue_assassination`
   - `rogue_subtlety`
   - `rogue_agility`
   - `templar_smite`
   - `templar_grace`
   - `templar_faith`
5. monster family namespace:
   - `beast.*`
   - `bandit.*`
   - `undead.*`
   - `orc.*`
   - `cultist.*`

约束：

1. Phase 2 先建稳定 id 和 schema 外壳，再在 `P2-W6`、`P2-W7` 填可玩内容。
2. 任何新增职业、技能、怪物、zone 都必须先走这一套 schema，而不是在代码里直接硬编码。

最小对象壳必须覆盖：

1. `ProfessionDef`
   - `resourceType`
   - `baseStats`
   - `statGrowth`
   - `startingResources`
   - `startingTalents`
   - `startingKit`
   - `talentTrees`
   - `unlockCondition`
   - `tags`
   - `soloContract`
2. `TalentDef`
   - `maxPoints`
   - `category`
   - `damageType`
   - `kind`
   - `iconKey`
   - `visualKey`
   - `audioProfile`
   - `cooldown`
   - `castTime`
   - `requirements`
   - `levelEffects`
   - `keywords`
   - `callbacks`
   - `telegraph`
   - `resourceCosts`
   - `targeting`
3. `TalentTreeDef`
   - `layout`
   - `nodes`
4. `DifficultyDef`
   - `id`
   - `nameKey`
   - `monsterHpMultiplier`
   - `monsterDamageMultiplier`
   - `xpMultiplier`
   - `lootRarityBonus`
   - `prerequisites`
5. `MonsterTemplateV2`
   - `archetype`
   - `visualKey`
   - `audioProfile`
   - `aiProfileId`
   - `lootProfileId`
   - `talents`
6. `BossEncounterDef`
   - `bossTemplateId`
   - `arenaId`
   - `phases`
   - `rewards`
7. `ZoneSpec V1`
   - `biome`
   - `floorCount`
   - `mapSize`
   - `recommendedLevel`
   - `environmentTheme`
   - `specialMechanics`
   - `tilesetKey`
   - `ambientProfile`
   - `monsterPools`
   - `elitePools`
   - `bossEncounterId`
   - `objectiveSetId`

elite / Boss 的 `aiProfileId` 在 Phase 2 就必须升级成可校验 schema，而不只是“保留一个裸字符串字段”。原因是 `P2-W6`、`P2-W7` 需要接入最小 scripted AI，而不是继续把首批精英/Boss 绑死在 `CHASE`。

### 4.4 Key 命名空间规则

推荐规则：

1. profession:
   - `profession.vanguard.name`
   - `profession.vanguard.desc`
2. talent:
   - `talent.vanguard.power_strike.name`
   - `talent.vanguard.power_strike.desc`
3. monster:
   - `monster.bandit.raider.name`
4. zone:
   - `zone.shattered_outpost.name`
   - `zone.shattered_outpost.desc`
5. ui:
   - `ui.menu.new_game`
   - `ui.inventory.title`

冻结口径：

1. key 命名空间必须一眼可追到对象族和对象 id。
2. `name` 与 `desc` 的 key 模式必须统一，禁止同类对象各写各的。

### 4.5 Locale 选择

建议文件：

```text
client/src/main/kotlin/com/ktome/client/menu/*
game/src/main/resources/i18n/*.json
```

冻结口径：

1. 首页 locale 选择必须影响新开局和读当前阶段存档。
2. 局内日志、菜单、HUD、背包、检视都走同一 locale 服务。
3. locale 切换是运行态能力，不要求热切换已开局文本缓存。

### 4.6 `locale-lint`

必须检查：

1. 缺 key
2. 多余 key
3. 占位符不一致
4. 空 desc/name
5. 不允许裸文本回流正式 schema

### 4.7 `contract-lint`

必须检查：

1. schema 字段齐全
2. `visualKey/audioProfile` 可解析
3. `id/nameKey` 唯一
4. `schemaVersion` 存在
5. `profession/talent/monster/zone/item` 的目录和命名空间合法
6. cross-reference 合法：
   - profession -> talentTrees
   - monster -> aiProfileId / lootProfileId / talents
   - bossEncounter -> bossTemplateId / arenaId
   - zone -> monsterPools / elitePools / bossEncounterId / tilesetKey / ambientProfile

## 5. 推荐改动面

### 5.1 `game`

1. schema loader
2. YAML 模型
3. i18n key 资源
4. profession/talent/monster/zone/item skeleton

### 5.2 `client`

1. 首页 locale 选择
2. 文本渲染入口

### 5.3 `tools`

1. `locale-lint`
2. `contract-lint`

## 6. 测试与自证

### 6.1 必测类

1. `SchemaV2LoaderTest`
2. `LocaleServiceTest`
3. `LocaleLintTest`
4. `ContractLintTest`
5. `ProfessionSchemaTest`
6. `TalentSchemaTest`
7. `MonsterSchemaTest`
8. `ZoneSchemaTest`

### 6.2 必测行为

1. Schema V2 数据可完整加载。
2. 首页切语言后，新开局文本正确。
3. 读当前阶段存档后文本能按当前 locale 重渲染。
4. 缺 key 或错误 key 时 lint 失败。
5. 首批 profession/talent/monster/zone skeleton 都可被 loader 正常识别。
6. cross-reference 缺失或悬挂时 lint 明确失败。

### 6.3 自动化命令

```bash
./gradlew :game:test
./gradlew localeLint
./gradlew contractLint
./gradlew test
```

### 6.4 白盒验证

1. 启动首页，切中文。
2. 新开局检查菜单、日志、背包、HUD 标题。
3. 切英文，再读存档。
4. 预期：
   - 文本按当前语言显示
   - 不依赖存档内旧字符串

## 7. 出口门禁

1. 正式 schema V2 生效。
2. 首页 locale 选择可用。
3. `locale-lint`、`contract-lint` 成为回归入口。
4. 正式内容不再新增裸文本。
5. Phase 2 首批正式对象族都有稳定 schema 和 key 命名基线。
6. profession/talent/monster/boss/zone 的 cross-reference 校验进入 `contract-lint`。

## 8. 风险与止损

1. 如果 loader 同时支持多套野生字段，必须先压缩到 V2 主路径。
2. 如果 i18n key 命名无纪律，必须先统一命名空间再继续加内容。
3. 如果 lint 只报警不失败，就不能作为门禁，必须补成 fail-fast。

## 9. 当前状态

1. 本文是 `P2-W3` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 的正式内容和语言路径才算可持续。

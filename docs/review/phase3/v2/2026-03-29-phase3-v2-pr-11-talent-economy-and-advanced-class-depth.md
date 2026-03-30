> 执行前必须先完整阅读并接受：
> `docs/phase3/2026-03-13-phase3-pr-03-talent-tree-v2-and-dynamic-descriptions.md`
> `docs/phase3/2026-03-13-phase3-pr-05-class-formalization.md`
> `docs/phase3/2026-03-13-phase3-deep-combat-classes-and-long-run.md`
> `docs/phase3/2026-03-13-phase3-verification-checklist.md`
> `docs/review/phase3/2026-03-26-phase3-pr-10-player-facing-information-cleanup.md`

# Phase 3 V2 - PR-11 Talent Economy Rebalance And Advanced Class Depth

**阶段**: `Phase 3 / v2 follow-up`  
**优先级**: `P0`  
**前置条件**: `PR-05` 已冻结职业/资源合同，`PR-10` 已完成玩家信息清理，允许进入体验密度修正  
**对应问题**: 当前 Phase 3 最硬的构筑债务其实是两条同源问题：天赋点经济过紧，导致基础职业 build 分化很弱；同时可 release 的进阶职业（`Berserker / Spellblade`）每职业只有 `6` 个天赋，进一步放大了“解锁后反而更浅”的落差。

**Lane-parallel 拆分**：

- **W11a (Rules Lane)**: talent point economy 合同收口，`ExperienceSystem` 改成正式的 Phase 3 长局节奏
- **W11b (Content Lane)**: `Berserker / Spellblade` talent tree 加厚到 release-ready floor
- **W11c (Tools/QA Lane)**: progression / long-run / advanced-class smoke 回归
- **W11d (Asset/Audio Lane)**: 新增 talent icon / visual / cue 接入正式资源管线

---

## 1. 阶段目标

把 Phase 3 的成长主链从“有 talent tree 但没有足够点数做选择”修正为“基础职业和 release-ready 进阶职业都能形成可辨认的 build 路线”。

完成标准：

1. 从 `Lv1 -> Lv20` 的总天赋点获取量固定提升到 `19`（每次升级 `+1 talent point`）。
2. stat point 口径保持不变，仍为每级 `+2`。
3. 基础职业在长局中可以稳定形成 `8~10` 个已学习 talent 的 build 形态，而不是被迫只走同一条主线。
4. `Berserker / Spellblade` 各自补到 `12` 个正式 talent（每棵树 `4` 个），不再是 `3 * 2` 的半成品。
5. `Shadowblade / Warden` 继续保持 frozen，不被本 PR 强行解冻。
6. 本 PR 不额外引入 route / boss / milestone talent point 来源，避免把成长口径分散成第二套真源。

## 2. 当前问题

1. [ExperienceSystem.kt](/Users/luo/Documents/github/K-ToME/core/src/main/kotlin/com/ktome/core/progression/ExperienceSystem.kt) 当前只在奇数级给天赋点，`Lv1 -> Lv20` 总计约 `10` 点。
2. 4 个基础职业每个都有 `15~16` 个 talent 节点，当前点数只够点出一条很窄的路径，Respec 的存在价值被压低。
3. [talents/index.yaml](/Users/luo/Documents/github/K-ToME/game/src/main/resources/data/talents/index.yaml) 中 `Berserker / Spellblade` 每棵树当前只有 `2` 个节点，总计 `6` 个 talent，release-ready 深度明显不足。
4. 若先进入后续 Phase 4 内容扩张，再回头改天赋点经济，会让所有新增 talent 和 build 平衡全部重算。

### 2.1 本 PR 必须冻结的口径

1. 等级上限仍固定为 `20`，本 PR 不改 XP 曲线。
2. 天赋点第一版固定为“每级 `+1`”，不引入 `Boss / Route / Achievement` 额外 talent point 来源。
3. stat point 仍固定为每级 `+2`。
4. talent rank cost 口径本 PR 不重写，仍沿用现有 rank cost / allocation contract。
5. `Berserker / Spellblade` 的目标 floor 固定为每职业 `12` 个 talent，不追求一步到位补到与基础职业完全同量级。
6. `Shadowblade / Warden` 继续维持 frozen contract，不因为本 PR 连带解冻。
7. 不引入新的 `DamageType / ResourceType / PowerType / TalentOp` 家族；新增 talent 优先复用现有 `EffectOp / DescriptionModel` 能表达的机制。

## 3. 范围与非目标

### 3.1 范围

1. `ExperienceSystem` talent point gain contract 调整。
2. `Berserker / Spellblade` tree 扩充到每棵树 `4` 节点。
3. 对应 schema / i18n / smoke / long-run 回归。
4. 必要的 talent allocation / progression 单测更新。

### 3.2 非目标

1. 不做属性 breakpoint 系统。
2. 不做 talent 数值大重做；只有在新增点数后出现明显爆表时才做最小必要平衡修正。
3. 不补 `Shadowblade / Warden` 的完整 talent 套。
4. 不改变职业解锁条件。
5. 不在本 PR 引入新的成长 UI 页面。

## 4. 技术方案

### 4.1 [W11a] Talent Point Economy Contract

建议文件：

```text
core/src/main/kotlin/com/ktome/core/progression/ExperienceSystem.kt
core/src/test/kotlin/com/ktome/core/progression/ExperienceSystemTest.kt
core/src/test/kotlin/com/ktome/core/talent/TalentAllocationDraftTest.kt
```

冻结口径：

1. `ExperienceSystem.applyReward()` 在每次合法升级时固定增加 `1` 点 `unspentTalentPoints`。
2. 不再使用 `level % 2 == 1` 作为天赋点门槛。
3. 建议新增显式 helper，例如：
   - `talentPointsGrantedForLevel(level: Int): Int = 1`
4. 该 helper 只作为单一真源，后续若要继续微调成长节奏，统一改这里，而不是把“额外 talent point”散落进 route / boss / objective。

建议验收断言：

1. `Lv1 -> Lv2` 获得 `1` 点 talent point。
2. `Lv1 -> Lv20` 总计获得 `19` 点 talent point。
3. stat point 仍为 `38`。
4. 已有的 health/resource restore on level up 语义不变。

### 4.2 [W11b] Advanced Class Depth Floor

建议文件：

```text
game/src/main/resources/data/talents/index.yaml
game/src/main/resources/data/professions/index.yaml
game/src/main/resources/i18n/en-US.json
game/src/main/resources/i18n/zh-CN.json
game/src/test/kotlin/com/ktome/game/data/ProfessionSchemaTest.kt
game/src/test/kotlin/com/ktome/game/ClassFormalizationRuntimeContractTest.kt
```

冻结口径：

1. `Berserker` 三棵树各补 `2` 个 talent，总计从 `6 -> 12`。
2. `Spellblade` 三棵树各补 `2` 个 talent，总计从 `6 -> 12`。
3. 新增 talent 必须围绕该职业已冻结的资源/玩法轴展开，不允许引入第二套资源解释：
   - `Berserker`: `HATE / burst / grit / kill-chain`
   - `Spellblade`: `MANA + EQUILIBRIUM / enchanted blade / flux control / melee spell`
4. 新 talent 优先复用现有机制族：
   - direct damage
   - line / cone / burst AoE
   - temporary self-buff
   - guard / shield
   - reposition / lunge
   - status apply / consume

建议的最小 talent floor：

| 职业 | 树 | 当前 | 目标 |
| --- | --- | --- | --- |
| Berserker | `wrath / ruin / bloodwar` | `2 / 2 / 2` | `4 / 4 / 4` |
| Spellblade | `enchanted_blade / elemental_flux / battle_spell` | `2 / 2 / 2` | `4 / 4 / 4` |

建议的新增方向：

1. `Berserker`
   - `wrath`: 补“高仇恨斩杀”与“追击连段”
   - `ruin`: 补“裂线/破阵”与“地面震荡”
   - `bloodwar`: 补“受伤反打”与“击杀续战”
2. `Spellblade`
   - `enchanted_blade`: 补“元素附刃”与“护甲/抗性撕裂”
   - `elemental_flux`: 补“平衡区间 payoff”与“失衡救场”
   - `battle_spell`: 补“近战位移施法”与“反制/招架 follow-up”

### 4.3 [W11c] Balance Safety And Verification

建议文件：

```text
game/src/test/kotlin/com/ktome/game/harness/LongRunLabTest.kt
game/src/test/kotlin/com/ktome/game/harness/LongRunLabFullTest.kt
game/src/test/kotlin/com/ktome/game/ClassFormalizationTestSupport.kt
```

冻结口径：

1. 本 PR 不通过“削减基础职业 talent 数量”来适配更高的点数。
2. 如果点数提升后 clear rate 显著过冲，优先微调 talent 数值，而不是回滚天赋点主线。
3. `Berserker / Spellblade` smoke 必须覆盖到至少一次终局到达，确保新增 talent 不是 schema-only。

建议新增的自动化检查：

1. `ExperienceSystemTest` 断言总 talent point 增长。
2. `ProfessionSchemaTest` 断言 `Berserker / Spellblade` 各有 `12` 个 talent。
3. `LongRunLab` 对比职业矩阵中 late-game 到达率不能数量级失真。

### 4.4 [W11d] Talent Asset And Audio Pack

建议文件：

```text
assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml
assets-src/audio/specs/phase3-v2-pr11-audio-plan.yaml
assets-src/image/manifests/phase3-v2-pr11-generation-report.jsonl
assets-src/audio/manifests/phase3-v2-pr11-processing-report.jsonl
game/src/main/resources/data/visuals/index.yaml
game/src/main/resources/data/audio/index.yaml
client/src/main/resources/manifests/visual-manifest.json
client/src/main/resources/manifests/audio-manifest.json
build.gradle.kts
```

冻结口径：

1. `PR-11` 新增 talent 资源继续沿用 [PR-09 Asset Batch Generation Checklist](/Users/luo/Documents/github/K-ToME/docs/review/phase3/2026-03-26-phase3-pr-09-asset-batch-generation-checklist.md) 与 [PR-09 Asset Namespace Table](/Users/luo/Documents/github/K-ToME/docs/review/phase3/2026-03-27-phase3-pr-09-asset-namespace-table.md) 的目录、命名和 manifest 规则，不再发明第二套资源口径。
2. 所有新增 talent 至少交付：
   - `talent.<profession>.<talent_id>.icon`
   - `icon.skill.<profession>.<talent_id>`
3. 所有主动施放型、带明确 burst/charge/lunge/stance-activate 行为的新增 talent，还必须交付：
   - `talent.<profession>.<talent_id>.visual`
   - `audio.talent.<talent_id>`
4. 被动、常驻 aura、纯数值增益型 talent 可以不新增独立 `.visual`，但不得缺 icon；音频允许复用同树的主动 talent cue，不允许正式 key 指向 `audio/fallback/silence.ogg`。
5. 图片生成仍只能走现有 `scripts/generate_assets.sh -> scripts/process_assets.py` 管线。真正启动图片批量生成前，执行方必须**主动向用户索取 `GEMINI_API_KEY`**；未提供 key 时，只允许继续编写 plan / manifest / prompt，不得开始生成。
6. `phase3-v2-pr11-gemini-plan.yaml` 中的每条正式图片 spec 必须设置 `geminiKeyRequired: true`；本 PR 同时必须把新的 image/audio plan 接到 root `assetLint / styleLint / audioLint / manifestLint` 的 `--extra-plan`，避免 CI 仍只校验 `PR-09`。
7. 正式 key 不得落到 `debug/missing_visual.png`、`audio/fallback/silence.ogg` 或 prefix fallback。

建议批次：

1. `Batch-11A Berserker Talent Pack`
   - `wrath` 树：黑铁、断口、猩红裂隙、追击冲势
   - `ruin` 树：碎石冲击、地面裂波、重击余震、阵线撕开
   - `bloodwar` 树：血痕、锁链、创伤反击、击杀续战
2. `Batch-11B Spellblade Talent Pack`
   - `enchanted_blade` 树：刻符刃锋、附魔火花、护甲剥离、近战魔刃
   - `elemental_flux` 树：火冰对冲、平衡区间 payoff、失衡回正、元素回卷
   - `battle_spell` 树：贴身施法、短距突进、招架反制、近战 follow-up
3. `Batch-11C Manifest And Runtime Landing`
   - `visuals/index.yaml` 先登记正式 key
   - `visual-manifest.json` 指向 `phase3/p3-followup/talent_<profession>_<talent_id>_visual.png` 与 `phase3/p3-followup/icon_skill_<profession>_<talent_id>.png`
   - `audio/index.yaml` / `audio-manifest.json` 登记 `audio.talent.<talent_id>`，原始音频继续落到 `client/src/main/resources/audio/talent/<talent_id>.ogg`

文件名冻结：

```text
phase3/p3-followup/talent_berserker_<talent_id>_visual.png
phase3/p3-followup/icon_skill_berserker_<talent_id>.png
phase3/p3-followup/talent_spellblade_<talent_id>_visual.png
phase3/p3-followup/icon_skill_spellblade_<talent_id>.png
audio/talent/<talent_id>.ogg
```

建议执行顺序：

1. 先冻结新增 `12` 个 talent id 与 active/passive 分类。
2. 先写 `phase3-v2-pr11-gemini-plan.yaml` / `phase3-v2-pr11-audio-plan.yaml`，再补 `visuals/audio index`。
3. 再更新 `visual-manifest.json` / `audio-manifest.json`。
4. 生成图片前先向用户索取 `GEMINI_API_KEY`；拿到 key 后再执行：

```bash
GEMINI_API_KEY=your_key ./scripts/generate_assets.sh \
  assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml \
  assets-src/image/raw/generated \
  assets-src/image/manifests/phase3-v2-pr11-generation-report.jsonl
```

5. 音频 raw 导入/清洗后执行：

```bash
python3 scripts/process_audio.py \
  --report assets-src/audio/manifests/phase3-v2-pr11-processing-report.jsonl
```

## 5. 推荐改动面

### 5.1 `core`

1. `ExperienceSystem.kt`
2. progression / talent allocation tests

### 5.2 `game`

1. `data/talents/index.yaml`
2. `data/professions/index.yaml`
3. i18n 文案
4. class/progression/schema tests

### 5.3 `asset/audio`

1. `assets-src/image/specs/phase3-v2-pr11-gemini-plan.yaml`
2. `assets-src/audio/specs/phase3-v2-pr11-audio-plan.yaml`
3. `game/src/main/resources/data/visuals/index.yaml`
4. `game/src/main/resources/data/audio/index.yaml`
5. `client/src/main/resources/manifests/visual-manifest.json`
6. `client/src/main/resources/manifests/audio-manifest.json`
7. `build.gradle.kts`

### 5.4 `tools / QA`

1. `soloClearLab`
2. `longRunLab`
3. advanced-class smoke 观察项

## 6. 测试与自证

### 6.1 必测类

1. `ExperienceSystemTest`
2. `TalentAllocationDraftTest`
3. `ProfessionSchemaTest`
4. `ClassFormalizationRuntimeContractTest`
5. `LongRunLabTest`
6. `LongRunLabFullTest`

### 6.2 必测行为

1. 每次升级都能获得 `1` 点 talent point。
2. `Lv1 -> Lv20` 总 talent point 为 `19`。
3. `Berserker / Spellblade` 的 talent tree 各为 `4 * 3 = 12` 节点。
4. 新增 talent 可以被正常分配、描述、装备和结算。
5. `Shadowblade / Warden` 仍不进入正式可玩主路径。

### 6.3 自动化命令

```bash
./gradlew :core:test --tests "com.ktome.core.progression.ExperienceSystemTest"
./gradlew :core:test --tests "com.ktome.core.talent.TalentAllocationDraftTest"
./gradlew :game:test --tests "com.ktome.game.data.ProfessionSchemaTest"
./gradlew :game:test --tests "com.ktome.game.ClassFormalizationRuntimeContractTest"
./gradlew assetLint
./gradlew styleLint
./gradlew audioLint
./gradlew manifestLint
./gradlew soloClearLab
./gradlew longRunLab
./gradlew check
```

### 6.4 白盒验证

1. 用任意基础职业升到 `Lv2 / Lv3 / Lv4`，确认每次升级都新增 `1` 点天赋点。
2. 用 `Berserker` 与 `Spellblade` 各开一局，确认 talent 面板不再只有每树 `2` 个节点。
3. 对新增 talent 至少各实际施放一次，确认说明与实际效果一致。
4. 打开 talent 面板与战斗施放界面，确认新增 icon / visual / cue 都已接正式资源，不再落到 `missing_visual` 或静音 fallback。

## 7. 出口门禁

1. `ExperienceSystem` talent point 口径已改为每级 `+1`。
2. `Berserker / Spellblade` 各自达到 `12` 个正式 talent。
3. 新增 talent 正式资源已接入 `visual/audio index + manifest`，并通过 `assetLint / styleLint / audioLint / manifestLint`。
4. `soloClearLab / longRunLab` 没有因为成长改动出现明显崩塌。
5. `./gradlew check` 绿色。

## 8. 风险与止损

### 8.1 风险

1. talent point 增加后，基础职业中后期可能出现数值过饱和。
2. `Berserker / Spellblade` 如果只补数量不补玩法差异，会变成“更长的同质列表”。
3. 把成长经济和进阶职业加厚放在同一 PR，会让 balancing 成本上升。

### 8.2 止损

1. 若 `W11b` 滑出周期，允许 `W11a` 先独立落地；但 `Berserker / Spellblade` 继续保持“非正式主路径”直到 talent depth 补完。
2. 新增 talent 优先复用现有 `EffectOp`，避免把风险转移成 runtime 机制扩张。
3. 若 clear rate 暴涨，优先调 talent 数值和 cooldown，而不是回退 talent point 主合同。

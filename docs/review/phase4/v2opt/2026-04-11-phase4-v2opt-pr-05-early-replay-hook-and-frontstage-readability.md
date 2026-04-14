> 执行前必须先完整阅读并接受：
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
> `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-pr-04-terrain-mutation-semantics-and-theme-hardening.md`
> `docs/phase4/2026-03-13-phase4-pr-02-hybrid-topology-pattern-vault-and-biome-family.md`
> `docs/review/phase4/opt/PR/2026-04-09-phase4-opt-pr-06-terrain-readability-and-tactical-uptake-tuning.md`

# Phase 4 - V2OPT PR-05 前 30 分钟 Replay Hook 与 Frontstage Readability

**阶段**: `Phase 4 / Post-Review Follow-up / V2OPT-W5`  
**优先级**: `P2`  
**工作量评估**: `M`（`2~4` 人日）  
**前置条件**: `V2OPT-PR-01 ~ 04` 已稳定；尤其是 growth/hidden/terrain 主线不再继续改合同  
**对应问题**:

1. `greenwood_fringe` 当前只刚好过 replayability 门槛
2. mutation / terrain / search / passive 的关键信息更多沉在 inspect / log 的二级通道
3. 当前版本已经“可解释”，但还没足够“低摩擦可感知”

## 0. PR-06 后的验证约束

1. 默认开发回路先跑 `./gradlew verifyChanged`。
2. 本 PR 的默认联合验收固定为：`whiteBoxMapgen`、`clientSmoke`、`goldenScreenshot`、`verifyOwner`、`phase4Report`。
3. `phase4Report` 的 canonical 输出路径固定为 `tools/build/reports/verification/phase4/report-phase4-summary.{json,md}`。
4. 只有当本 PR 改到 verification contract、baseline、aggregation schema 或 report schema 时，才额外执行 `phase4LegacyReportOnly + reportPhase4` 做显式 parity 对账。
5. legacy `tools/build/reports/phase4/phase4-summary.{json,md}` 在本 PR 中只属于手工 fallback/historical artifact，不得再作为默认验收产物。
6. 提交或审查本 PR 时，默认证据必须直接引用 canonical `report-phase4-summary.{json,md}` 与对应 white-box / golden artifact 中的段落或 metric：
   - `greenwood_fringe` replay hook 相关 mapgen 差异指标
   - terrain / mutation / hidden / passive 前台摘要对应截图或 golden 结果
   - `clientSmoke` / `goldenScreenshot` 的 canonical 产物或差异结论
   不能只报命令 exit code，也不能只贴 legacy `phase4-summary` 快照。
7. 若本 PR 变更了 replayability/readability 相关 owner metric、render snapshot 字段、golden 语义或 report schema，除跑显式 parity 外，还必须在同一提交同步更新：
   - `docs/review/phase4/v2opt/README.md`
   - `docs/review/phase4/v2opt/2026-04-11-phase4-v2opt-verified-next-pr-plan.md`
   - 若影响 Phase 4 正式 gate 口径，再同步 `docs/phase4/2026-03-13-phase4-verification-checklist.md`、`docs/phase4/roadmap.md` 与相邻 authority docs
8. 若本 PR 需要新增或调整 replayability/readability 相关 aggregate/report task、兼容 alias 或 report-only 入口，必须复用统一 helper / declarative generation path；不得复制新的 `tasks.register<Test>` family。
9. 相关 build contract test 必须直接断言 helper 参数与 canonical/parity/fallback 语义；若 canonical 与 parity 共用输出目录，plain `phase4Report` 后不得残留 parity-only artifact。

---

## 1. 阶段目标

把 Phase 4 的前 30 分钟从“系统开始成立”推进到“玩家很快能感觉这局会不一样”。

完成标准：

1. `greenwood_fringe` 的 mapgen 差异不再只刚好过线。
2. terrain / mutation / search / passive 的高价值信息更靠前。
3. 不引入大 UI 系统，只做低成本高信号增强。

---

## 2. 工作量评估与整合结论

### 2.1 为什么这个 PR 放最后

当前更深的结构债是：

1. growth identity
2. organic hidden
3. reward identity
4. terrain / mutation semantics

这些没修之前，做前台抛光只会把“底层还不稳”的问题放大到玩家面前。  
因此本 PR 只在前四个 PR 收口后做。

### 2.2 为什么 mapgen 与前台反馈合并

如果拆开：

1. 只做 `greenwood` pattern/layout，会提升 replay hook，但玩家不一定看得见。
2. 只做前台反馈，不补 early zone 差异，玩家仍然会觉得“前十分钟没什么新东西”。

这两个改动都服务同一目标：**更早让玩家感到每局不同。**

---

## 3. 当前问题拆解

### 3.1 `greenwood_fringe` 的 replay hook 仍偏弱

当前 `whitebox-mapgen-summary.json` 中：

1. `distinctPatternRoomCount = 1`
2. `distinctEntranceLayoutCount = 1`
3. `differenceCategoryCount = 3`

这是“过线”，不是“强 replay hook”。

### 3.2 关键信息仍偏二级通道

当前系统不是没有信息，而是：

1. terrain 需要 inspect 才看清
2. mutation 需要目标 inspect 才能完整理解
3. passive 触发更多是日志可追溯，而不是前台可见
4. search / hidden 成功反馈有，但仍不够前台

---

## 4. 本 PR 必须冻结的合同

1. 不新增大 UI 系统。
2. 不重开新的教程系统。
3. 前台提示只服务已存在的正式合同：
   - terrain
   - mutation
   - passive
   - search / hidden
4. `greenwood_fringe` 的 replay hook 增强不能依赖新规则系统。

---

## 5. 范围与非目标

### 5.1 范围

1. `greenwood_fringe` 的 pattern/layout 扩充
2. terrain / mutation / passive / search 的前台摘要
3. 对应 i18n、render、golden screenshot

### 5.2 非目标

1. 不再改 loot 分发合同
2. 不再改 hidden reward 合同
3. 不再改 elite/boss 选择逻辑

---

## 6. 技术方案

### 6.1 `greenwood_fringe` replay hook 增强

优先做：

1. 新增至少 1 个 pattern room
2. 新增至少 1 个 entrance layout family
3. 让 early hidden signal 更鲜明

建议落点：

```text
game/src/main/resources/data/mapgen/patterns/*.yaml
game/src/main/resources/data/mapgen/vaults/*.yaml
game/src/main/resources/data/mapgen/zones/index.yaml
```

原则：

1. 不新增新地形系统
2. 不新增新 hidden contract
3. 只增强前 30 分钟的差异可感知度

### 6.2 Frontstage mutation / terrain 摘要

建议优先放到：

1. 当前目标敌人摘要
2. 当前站位 tile 摘要
3. combat feedback 浮层或目标条补充

需要可见的内容：

1. 敌人当前 mutation 核心摘要
2. 当前格 terrain 危险/收益
3. 最近一次关键 passive / terrain trigger

### 6.3 Search / hidden 前台反馈

最小增强：

1. 成功 reveal：强反馈
2. 失败但有内容：明确是“你没过检定”
3. 进入 secret zone：来源和价值感更明显

### 6.4 Passive 前台可读性

当前 passive 已有 inspect / log；本 PR 只做“前台摘要”，不重写 passive 系统。

建议：

1. 最近一次关键 passive trigger 的短摘要
2. 装备驱动的特殊效果在奖励预览里更明显

---

## 7. 推荐改动面

### 7.1 `game` 数据

1. `game/src/main/resources/data/mapgen/patterns/*.yaml`
2. `game/src/main/resources/data/mapgen/vaults/*.yaml`
3. `game/src/main/resources/data/mapgen/zones/index.yaml`
4. 必要时 hidden 相关 i18n

### 7.2 `client`

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
2. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`

### 7.3 `game` runtime

1. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
2. reward / inspect / combat feedback snapshot 映射路径

### 7.4 `tests`

1. `whiteBoxMapgen`
2. `goldenScreenshot`
3. `clientSmoke`

---

## 8. 实施顺序

### Task 1：greenwood pattern / layout 扩容

- **目标**：让 `greenwood` 不再只刚好过线
- **验收**：
  - `distinctPatternRoomCount` 提升
  - `distinctEntranceLayoutCount` 提升

### Task 2：terrain / mutation 前台摘要

- **目标**：把最重要信息从二级通道拉到前台
- **文件**：`TileRenderModel.kt`, `AsciiRenderModel.kt`, `FoundationGameSession.kt`

### Task 3：search / hidden 成败反馈增强

- **目标**：降低理解成本

### Task 4：passive 短摘要

- **目标**：让装备效果更容易被立即感知

### Task 5：golden / white-box 回归

- **目标**：确认可读性增强没有破坏现有表现合同

---

## 9. 资源生成计划

### 9.1 图片

本 PR 不新增图片资源。

### 9.2 音频

本 PR 不新增音频资源。

### 9.3 复用基线

1. terrain frontstage 摘要复用现有 terrain 资源：
   - `vfx.terrain.interaction.water / oil / ice / oil_burning`
   - 相关 `audio.terrain.*`
2. mutation 摘要复用现有 mutation 图标 / cue：
   - `icon.mutation.*`
   - `audio.mutation.*`
3. hidden / search 前台反馈复用现有 hidden/secret 资源：
   - `prop.hidden_entrance.revealed`
   - `audio.hidden.reveal.secret_entrance`
   - `zone.secret.*.(icon|visual)`
   - `audio.secret_zone.*`
4. passive/readability 摘要继续复用现有 item 资源：
   - `item.unique.*.(icon|visual)`
   - `item.artifact.*.(icon|visual)`
   - `audio.item.unique.*`
   - `audio.item.artifact.*`

### 9.4 约束

1. 本 PR 的“frontstage readability”是把既有资源更早、更低摩擦地展示出来，不是再定义一套 badge/icon/audio 新体系。
2. `greenwood_fringe` replay hook 增强只允许通过 pattern/layout/secret signal 差异完成，不新增新 tile set、landmark art 或 zone-only ambience。
3. 若实现过程中出现“必须新开资源批次才能做前台摘要”的说法，默认先回到 render/snapshot 设计复审；在 `v2opt` 范围内不接受。

---

## 10. 测试策略

### 9.1 自动化命令

```bash
./gradlew whiteBoxMapgen
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew verifyOwner
./gradlew phase4Report
```

### 9.2 必测行为

1. `greenwood_fringe` 的 replay hook 指标提升
2. client 渲染无回归
3. golden screenshot 更新后稳定
4. 前台信息增强不造成明显噪音

### 9.3 白盒验证

1. 连续看 `5` 个不同 seed 的 `greenwood`，应更容易感知差异。
2. 观察一场有 mutation 和 terrain 的战斗，不依赖 inspect 也能知道核心危险点。
3. 做一次 search 失败和一次 search 成功，反馈都要足够明确。

---

## 11. 出口门禁

1. `greenwood_fringe` replay hook 指标高于当前基线。
2. golden screenshot / clientSmoke 全部通过。
3. terrain / mutation / hidden / passive 的前台摘要进入正式渲染路径。

---

## 12. 风险与 Gotchas

1. **不要把 UI 做成信息墙**  
   目标是更快感知，不是展示全部信息。
2. **不要把 `greenwood` 变成小型 `underground_river`**  
   early-game zone 仍要保留低复杂度。
3. **不要在这个 PR 再动深层平衡**  
   这里只做 replay hook 和可读性。

---

## 13. 回滚策略

1. 若前台提示过吵，优先回退前台摘要，不回退数据差异增强。
2. 若 `greenwood` 布局扩容影响 solvability 或 early difficulty，优先回退新增 pattern/layout，而不是继续堆临时例外逻辑。

> 执行前必须先完整阅读并接受：
> `docs/rule/kotlin.md`
> `docs/rule/ai-change-governance.md`
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

## 6. PR 级执行方式

### 6.1 单文档执行原则

1. 本文档本身就是本 PR 的执行合同；默认不再为 `Task 1 ~ 5` 另开子文档。
2. 第 `9` 节等价于把 `ktome-change-discipline` 内嵌进本 PR：开始某个 task 前，直接对照该 task 的 `Owner / Contracts`、`Chosen Shape`、`Forbidden Shortcuts Check`、`Deletion Budget` 即可，不需要再单独生成一份子方案。
3. 为了保持 PR 级流程紧凑，`Task 2 ~ 4` 若都仍落在同一条 `game -> client` readability boundary 上，允许作为一个连续实现批次推进；但 `Task 1` 和 `Task 5` 不应并入，因为它们分别属于 mapgen/content owner 与 verification close-out。
4. 若只是局部实现细化，不需要升级流程；只有当 change shape、稳定合同或验证面发生实质变化时，才触发 `Implementation Adjustment`。

### 6.2 默认执行回路

1. 选定当前 task，先确认它仍然落在本 PR 的范围与非目标内。
2. 开始编码前，快速过一遍该 task 的作者侧约束，不再额外开文档。
3. 默认最小回路先跑 `./gradlew verifyChanged`。
4. 若本次改动触及 non-trivial Kotlin 结构/边界调整、`game -> client` snapshot/readability contract、`tools` gate wiring 或其他 anti-bloat 高风险形状，再额外跑 `./gradlew maintainabilityLint`。
5. 然后补该 task 的最小验证；不要等全部 task 做完再第一次跑 owner 验证。
6. 收尾证据只引用 canonical `report-phase4-summary.{json,md}`、white-box 与 golden 产物，不引用 legacy fallback 快照作为默认完成证明。

### 6.3 Implementation Adjustment 规则

1. 若实现过程中发现 `Chosen Shape` 改变、命中新稳定合同，或出现新的 shortcut 压力，直接在本文档追加一段 `Implementation Adjustment` 记录。
2. 记录最少包含：变化原因、新的 owner/contract、额外 validation、是否影响范围/非目标。
3. 只有当调整越过本 PR 非目标、需要新核心抽象族，或必须改写 Phase 4 authority/checklist 口径时，才升级为新 PR 文档，而不是在本 PR 内继续扩张。

---

## 7. 技术方案

### 7.1 `greenwood_fringe` replay hook 增强

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

### 7.2 Frontstage mutation / terrain 摘要

建议优先放到：

1. 当前目标敌人摘要
2. 当前站位 tile 摘要
3. combat feedback 浮层或目标条补充

需要可见的内容：

1. 敌人当前 mutation 核心摘要
2. 当前格 terrain 危险/收益
3. 最近一次关键 passive / terrain trigger

### 7.3 Search / hidden 前台反馈

最小增强：

1. 成功 reveal：强反馈
2. 失败但有内容：明确是“你没过检定”
3. 进入 secret zone：来源和价值感更明显

### 7.4 Passive 前台可读性

当前 passive 已有 inspect / log；本 PR 只做“前台摘要”，不重写 passive 系统。

建议：

1. 最近一次关键 passive trigger 的短摘要
2. 装备驱动的特殊效果在奖励预览里更明显

---

## 8. 推荐改动面

### 8.1 `game` 数据

1. `game/src/main/resources/data/mapgen/patterns/*.yaml`
2. `game/src/main/resources/data/mapgen/vaults/*.yaml`
3. `game/src/main/resources/data/mapgen/zones/index.yaml`
4. 必要时 hidden 相关 i18n

### 8.2 `client`

1. `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
2. `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`

### 8.3 `game` runtime

1. [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
2. reward / inspect / combat feedback snapshot 映射路径

### 8.4 `tests`

1. `whiteBoxMapgen`
2. `goldenScreenshot`
3. `clientSmoke`

---

## 9. 实施顺序与内嵌 Author Discipline

### Task 1：greenwood pattern / layout 扩容

- **目标**：让 `greenwood` 不再只刚好过线
- **Owner / Contracts**：`game` 内容装配；命中 `mapgen pattern / vault / zone` 与 `whiteBoxMapgen` replay hook 指标口径，不引入新规则系统
- **Chosen Shape**：`content/schema wiring`
- **Forbidden Shortcuts Check**：
  - 不新增 zone-only compat path
  - 不用“特殊 seed 白名单”或 test-only patch 拉高 mapgen 指标
  - 不新开 terrain / hidden 第二套合同来补 replay hook
- **Deletion Budget**：把 `greenwood_fringe` 当前“仅靠门槛过线”的 replay hook 收口到正式 pattern/layout 差异里，避免后续继续依赖前台提示去掩盖地图差异不足
- **文件**：
  - `game/src/main/resources/data/mapgen/patterns/*.yaml`
  - `game/src/main/resources/data/mapgen/vaults/*.yaml`
  - `game/src/main/resources/data/mapgen/zones/index.yaml`
- **最小验证**：
  - `./gradlew verifyChanged`
  - `./gradlew whiteBoxMapgen`
  - `./gradlew phase4Report`
- **验收**：
  - `distinctPatternRoomCount` 提升
  - `distinctEntranceLayoutCount` 提升
  - `differenceCategoryCount` 不再只是门槛线

### Task 2：terrain / mutation 前台摘要

- **目标**：把最重要信息从二级通道拉到前台
- **Owner / Contracts**：`game -> client` 正式 readability boundary；命中 `RenderSnapshot` / combat feedback / target summary 的正式消费路径
- **Chosen Shape**：`extend boundary`
- **Forbidden Shortcuts Check**：
  - 不在 `client` 维护 terrain / mutation 第二真源
  - 不新增 `Boolean` 参数切换“摘要模式”
  - 不新增 `Helper / Manager` 汇总层去绕正式 snapshot 边界
- **Deletion Budget**：减少 terrain / mutation 只能靠 inspect / log 理解的依赖；不再为同一批正式信息额外补一套新 badge / tutorial 体系
- **文件**：
  - `client/src/main/kotlin/com/ktome/client/render/TileRenderModel.kt`
  - `client/src/main/kotlin/com/ktome/client/render/AsciiRenderModel.kt`
  - [FoundationGameSession.kt](/Users/luo/Documents/github/K-ToME/game/src/main/kotlin/com/ktome/game/FoundationGameSession.kt)
- **最小验证**：
  - `./gradlew verifyChanged`
  - `./gradlew clientSmoke`
  - `./gradlew goldenScreenshot`
  - `./gradlew verifyOwner`
  - 若新增 non-trivial snapshot/readability Kotlin 边界：`./gradlew maintainabilityLint`
- **验收**：
  - 不依赖 inspect，也能看出目标的 mutation 核心危险点
  - 当前格 terrain 的危险/收益进入正式前台路径
  - 没有把 UI 做成新的信息墙

### Task 3：search / hidden 成败反馈增强

- **目标**：降低理解成本
- **Owner / Contracts**：`game -> client` 的 search / reveal / secret entry feedback 正式路径；命中 hidden 成败反馈、secret zone 进入提示与既有 log token/feedback snapshot
- **Chosen Shape**：`extend boundary`
- **Forbidden Shortcuts Check**：
  - 不新增 primer / reveal shortcut 当成正式前台反馈来源
  - 不在 `client` 侧维护 hidden 成败的独立状态机
  - 不通过临时 branch patch 为单一 zone 特判前台文案
- **Deletion Budget**：收敛“失败但有内容”和“这里根本没东西”的模糊反馈，避免后续继续堆解释性提示或一次性例外逻辑
- **文件**：
  - search / hidden 对应 snapshot 映射路径
  - 相关 render / feedback 消费路径
- **最小验证**：
  - `./gradlew verifyChanged`
  - `./gradlew clientSmoke`
  - `./gradlew goldenScreenshot`
  - `./gradlew verifyOwner`
  - 若 search / hidden feedback contract 扩张到新的 Kotlin 边界：`./gradlew maintainabilityLint`
- **验收**：
  - success reveal 有强反馈
  - fail-but-present 与 no-content 能被玩家直接区分
  - 进入 secret zone 时来源和价值感更明显

### Task 4：passive 短摘要

- **目标**：让装备效果更容易被立即感知
- **Owner / Contracts**：`game -> client` 的 passive trigger 前台摘要；只消费既有 passive / effect / reward preview 正式合同，不重开 passive 系统
- **Chosen Shape**：`extend boundary`
- **Forbidden Shortcuts Check**：
  - 不新开 passive badge / mini-system
  - 不用额外 helper 层把 inspect/log 重新拼成第二套真源
  - 不为单个 item family 加临时 compat summary
- **Deletion Budget**：减少 passive 价值只能靠日志回看或奖励预览猜测的成本，不新增第二套 item/readability 语义
- **文件**：
  - reward / inspect / combat feedback snapshot 映射路径
  - `TileRenderModel.kt`
  - `AsciiRenderModel.kt`
- **最小验证**：
  - `./gradlew verifyChanged`
  - `./gradlew clientSmoke`
  - `./gradlew goldenScreenshot`
  - `./gradlew verifyOwner`
  - 若 passive 摘要新增 non-trivial Kotlin 模型或跨文件 API：`./gradlew maintainabilityLint`
- **验收**：
  - 最近一次关键 passive trigger 能被立即感知
  - 装备驱动效果在奖励预览里更容易看出来
  - 没有把 passive 前台化做成另一套规则说明系统

### Task 5：golden / white-box 回归

- **目标**：确认可读性增强没有破坏现有表现合同
- **Owner / Contracts**：`tools` owner gate、golden screenshot、canonical `phase4Report` 证据链
- **Chosen Shape**：默认 `inline simplify`；若动到 report schema / aggregation contract，则升级为 `extend boundary`
- **Forbidden Shortcuts Check**：
  - 不新增 sibling aggregate/report task family
  - 不把 legacy artifact 重新拉回默认验收
  - 不让 canonical 输出目录残留 parity-only artifact
- **Deletion Budget**：收掉“只报命令 exit code 或只贴 legacy 快照”的弱证据习惯，把完成证明统一收敛到 canonical 产物
- **文件**：
  - `whiteBoxMapgen`
  - `goldenScreenshot`
  - `clientSmoke`
  - `verifyOwner`
  - `phase4Report`
- **最小验证**：
  - `./gradlew verifyChanged`
  - `./gradlew whiteBoxMapgen`
  - `./gradlew clientSmoke`
  - `./gradlew goldenScreenshot`
  - `./gradlew verifyOwner`
  - `./gradlew phase4Report`
  - 若动到 verification contract / baseline / aggregation schema / report schema：额外执行 `phase4LegacyReportOnly + reportPhase4` parity 对账
  - 若包含 non-trivial Kotlin gate wiring：`./gradlew maintainabilityLint`
- **验收**：
  - canonical 产物齐全且可引用
  - 可读性增强没有破坏现有表现合同
  - parity-only artifact 不污染默认输出目录

---

## 10. 资源生成计划

### 10.1 图片

本 PR 不新增图片资源。

### 10.2 音频

本 PR 不新增音频资源。

### 10.3 复用基线

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

### 10.4 约束

1. 本 PR 的“frontstage readability”是把既有资源更早、更低摩擦地展示出来，不是再定义一套 badge/icon/audio 新体系。
2. `greenwood_fringe` replay hook 增强只允许通过 pattern/layout/secret signal 差异完成，不新增新 tile set、landmark art 或 zone-only ambience。
3. 若实现过程中出现“必须新开资源批次才能做前台摘要”的说法，默认先回到 render/snapshot 设计复审；在 `v2opt` 范围内不接受。

---

## 11. 测试策略

### 11.1 自动化命令

```bash
./gradlew verifyChanged
./gradlew whiteBoxMapgen
./gradlew clientSmoke
./gradlew goldenScreenshot
./gradlew verifyOwner
./gradlew phase4Report
```

若本 PR 的实际实现触及 non-trivial Kotlin 结构/边界调整、snapshot/readability contract 或 verification wiring，还必须额外执行：

```bash
./gradlew maintainabilityLint
```

### 11.2 必测行为

1. `greenwood_fringe` 的 replay hook 指标提升
2. client 渲染无回归
3. golden screenshot 更新后稳定
4. 前台信息增强不造成明显噪音

### 11.3 白盒验证

1. 连续看 `5` 个不同 seed 的 `greenwood`，应更容易感知差异。
2. 观察一场有 mutation 和 terrain 的战斗，不依赖 inspect 也能知道核心危险点。
3. 做一次 search 失败和一次 search 成功，反馈都要足够明确。

---

## 12. 出口门禁

1. `greenwood_fringe` replay hook 指标高于当前基线。
2. golden screenshot / clientSmoke 全部通过。
3. terrain / mutation / hidden / passive 的前台摘要进入正式渲染路径。

---

## 13. 风险与 Gotchas

1. **不要把 UI 做成信息墙**  
   目标是更快感知，不是展示全部信息。
2. **不要把 `greenwood` 变成小型 `underground_river`**  
   early-game zone 仍要保留低复杂度。
3. **不要在这个 PR 再动深层平衡**  
   这里只做 replay hook 和可读性。

---

## 14. 回滚策略

1. 若前台提示过吵，优先回退前台摘要，不回退数据差异增强。
2. 若 `greenwood` 布局扩容影响 solvability 或 early difficulty，优先回退新增 pattern/layout，而不是继续堆临时例外逻辑。

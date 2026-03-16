# Phase 4 Director Review — Round 5

**日期**：2026-03-16
**审阅视角**：资深游戏设计与开发总监
**审阅范围**：

1. `docs/phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md`（1085 行）
2. `docs/phase4/2026-03-13-phase4-verification-checklist.md`（178 行）
3. `docs/phase4/roadmap.md`（79 行）
4. `docs/2026-03-13-core-systems-design-and-phase-supplements.md`（§9.3）

**审阅焦点**：R4 修复确认 + 示例间语义一致性最终扫描。

---

## 0. R4 修复确认

| R4 编号 | 问题 | 状态 | 说明 |
| --- | --- | --- | --- |
| P2-R4-1 | §4.1.3 环路比例约束未同步 Checklist | ✅ 已修复 | §4.1.3 规则 4 已改为"若 `optionalLoopCount > 0`，则环路边数量占总连通边数量的比例控制在 `0.15 ~ 0.35`"，与 Checklist §2.1 一致 |
| P2-R4-2 | discoveryRule / entryRule 冗余 | ✅ 已修复 | 冻结口径 5 已补充一致性约束："`SecretZoneDef.entryRule` 必须与对应 `HiddenEntranceDef.discoveryRule` 一致，`SolvabilityHarness` 需要校验两者匹配" |
| P2-R4-3 | P4-W5 依赖缺 P4-W4 | ✅ 已修复 | 主文档 §3.3 执行约束 4 + §5.2 工作包表 + roadmap §4 均已统一为 `P4-W3, P4-W4`，并分层说明 loader/lint 可在 W3 后起步、headless harness 依赖 W4 |
| P2-R4-4 | terrainTagWeights 合并规则未说明 | ✅ 已修复 | §4.1.1 新增三步合并规则：biome 默认 → zone 显式覆盖 → 未声明 tag 沿用 biome |
| P2-R4-5 | key-gate 数据流未建模 | ✅ 已修复 | `TopologyEdge` 新增 `requiredKeys: Set<String> = emptySet()`；§4.2.2 构建路径新增"key-gate placement 的真源是 `TopologyEdge.requiredKeys`；`TopologyNode.tags` 只承载内容标签，不编码主线钥匙语义" |
| P2-R4-6 | guaranteedContent 格式未文档化 | ✅ 已修复 | 冻结口径 6 已写明 `{registryName}.{entryId}` 格式，并要求 registryName 与 overlay registry 名称一致 |
| P2-R4-7 | content pack 示例 ID 与 base game 冲突 | ✅ 已修复 | overlay targetId 改为 `sample_flooded_relics.sunken_altar`，带独立 namespace 前缀 |
| P2-R4-8 | VaultPlacement.roomId 与 nodeId 重叠 | ✅ 已修复 | `roomId` 重命名为 `roomDefId`，语义为 vault 对应的房间模板 ID，与 `nodeId`（拓扑节点实例 ID）区分 |
| P2-R4-9 | sunken_shrine 未定义 | ✅ 已修复 | `drowned_altar` 事件的 `REVEAL_SECRET_ZONE` reward 改为引用已定义的 `forgotten_cache` |

**R4 总计 9 项 P2 全部已确认修复。**

---

## 1. R5 新发现

### 新发现统计

| 等级 | 数量 |
| --- | --- |
| P0 | 0 |
| P1 | 0 |
| P2 | 3 |

---

### P2-R5-1：`drowned_altar` 事件在 `forgotten_cache` 的 `guaranteedContent` 中形成循环引用

**位置**：§4.2.3 SecretZoneDef YAML + §4.4.3 HiddenEventDef YAML

**问题**：R4-P2-9 修复将 `drowned_altar` 的 reward 从 `sunken_shrine`（未定义）改为 `forgotten_cache`（已定义），消除了孤立引用。但修复后形成了一个语义循环：

```
forgotten_cache (SecretZoneDef)
  guaranteedContent:
    - hidden_event.drowned_altar    ← 事件是 zone 的"保证内容"

drowned_altar (HiddenEventDef)
  conditions:
    - ZONE_ID: underground_river    ← 触发条件指向主 zone，不是 secret zone
  rewards:
    - REVEAL_SECRET_ZONE: forgotten_cache  ← 揭示它所在的 zone
```

矛盾点：

1. **`guaranteedContent` 的语义**：冻结口径 3 说"secret zone 的内容最少包含以下之一"——"包含"意味着内容物理上在 secret zone 内部。但 `drowned_altar` 的 `ZONE_ID: underground_river` 条件表明它在主 zone 触发，不在 `forgotten_cache` 内部。
2. **循环揭示**：secret zone 的保证内容中包含一个"揭示自身"的事件，逻辑上等同于"进入 zone 才能触发揭示 zone 的事件"——这是不可达的。

**建议**：最简修复——将 `hidden_event.drowned_altar` 从 `forgotten_cache` 的 `guaranteedContent` 中移除，只保留 `vault.flooded_cache`。`drowned_altar` 是主 zone 的发现触发器，不是 secret zone 的内部内容：

```yaml
# forgotten_cache — secret zone 内部保证内容
guaranteedContent:
  - vault.flooded_cache

# drowned_altar — underground_river 主 zone 的发现事件，不属于 secret zone 内部
```

---

### P2-R5-2：`roomTagFilter` 与 `allowedRoomTags` 的交互规则未文档化

**位置**：§4.1.1 ZoneMapgenProfile + §4.1.2 BiomeFamilyDef

**问题**：本轮修复为 `terrainTagWeights` 补充了明确的三步合并规则（biome 默认 → zone 覆盖）。但 `roomTagFilter` 和 `allowedRoomTags` 存在同类问题且未获得同等处理：

```kotlin
data class ZoneMapgenProfile(
    val roomTagFilter: Set<String>,       // zone 级房间标签过滤
)

data class BiomeFamilyDef(
    val allowedRoomTags: Set<String>,     // biome 级允许的房间标签
)
```

YAML 示例中两者使用了完全相同的值 `[ambush, shrine, hidden_cache]`，掩盖了"当两者不同时如何处理"的问题：

- 取交集？（biome 允许 `[ambush, shrine]`，zone 过滤 `[shrine, hidden_cache]`，结果只有 `[shrine]`）
- zone 覆盖 biome？
- biome 作为上限、zone 在其内选择子集？

**建议**：补充一句合并规则，建议采用"biome 定义允许上限，zone 在其内选择子集"的模式，与 terrainTagWeights 的合并规则风格一致：

> mapgen room 选择时，先以 `BiomeFamilyDef.allowedRoomTags` 为候选上限，再以 `ZoneMapgenProfile.roomTagFilter` 从中过滤；最终候选集为两者交集。

---

### P2-R5-3：`REVEAL_SECRET_ZONE` 奖励语义与单入口约束之间的关系未说明

**位置**：§4.2.3 冻结口径 5 + §4.4.3 HiddenEventRewardKey

**问题**：冻结口径 5 规定"Phase 4 默认每个 secret zone 只有一个入口"。同时 `HiddenEventRewardKey` 包含 `REVEAL_SECRET_ZONE`，表示 hidden event 可以揭示一个 secret zone。

两者的关系没有被文档化：

- 如果 `REVEAL_SECRET_ZONE` 意味着"使现有入口可见"（reveal ≠ create entrance），则单入口约束不受影响——事件只是让玩家知道入口存在。
- 如果 `REVEAL_SECRET_ZONE` 意味着"开启一条新的进入路径"，则等同于创建第二个入口，与单入口约束冲突。

当前示例中，`forgotten_cache` 同时拥有物理入口 `moss_wall_crevice`（PERCEPTION_CHECK）和事件揭示 `drowned_altar`（REVEAL_SECRET_ZONE）。如果两者都能让玩家进入 zone，则实际上有两条发现路径。

**建议**：在冻结口径 5 中补充一句，明确 `REVEAL_SECRET_ZONE` 的语义边界：

> `REVEAL_SECRET_ZONE` 只使对应 secret zone 的入口在地图上可见（等同于自动通过 discovery check），不创建新的物理入口。每个 secret zone 仍只有一个 `HiddenEntranceDef` 定义的物理入口。

---

## 2. 跨文档一致性检查

### 2.1 核心文档 §9.3

| 检查点 | 状态 |
| --- | --- |
| 类型名统一 | ✅ |
| 权威链声明 | ✅ |
| 简化骨架不引入新冲突 | ✅ |
| `TopologyEdge` 新增 `requiredKeys` 不影响核心文档（权威链已覆盖） | ✅ |

### 2.2 Phase 4 内部 YAML ↔ Kotlin 一致性

| 数据类型 | 状态 | 备注 |
| --- | --- | --- |
| ZoneMapgenProfile | ✅ | 7 字段 + YAML 完全匹配 |
| VaultDef | ✅ | 含 `requiredTerrainTags` |
| BiomeFamilyDef | ✅ | 5 字段完全匹配 |
| HiddenEntranceDef | ✅ | 嵌套 discoveryRule 块匹配 |
| SecretZoneDef | ✅ | 5 字段完全匹配 |
| AffixCost | ✅ | 4 字段完全匹配 |
| HiddenEventDef | ✅ | typed conditions/rewards 匹配 |
| ContentPackManifest | ✅ | 10 字段完全匹配 |
| OverlayEntry | ✅ | 4 字段完全匹配 |

**YAML ↔ Kotlin 一致性：全部通过。**

### 2.3 公式变量闭合

**全部通过，与 R4 一致。**

### 2.4 主文档 ↔ Roadmap 一致性

| 检查点 | 状态 |
| --- | --- |
| 工作包依赖表一致（含 W5 → W3, W4） | ✅ |
| 执行原则措辞一致（loader/lint vs headless harness 分层） | ✅ |
| 检查点进入/退出条件一致 | ✅ |

### 2.5 主文档 ↔ Checklist 一致性

| 检查点 | 状态 |
| --- | --- |
| 环路约束条件化（optionalLoopCount > 0） | ✅ |
| loot balance 测试矩阵含 magicFind=1.50 clamp 场景 | ✅ |
| mapgenSmoke / solvabilityHarness / lootBalanceLab / hiddenContentHarness / contentPackHarness 命令一致 | ✅ |

---

## 3. R5 总结

Phase 4 文档体系经过五轮审阅，质量演进如下：

| 轮次 | P0 | P1 | P2 | 总计 | 核心主题 |
| --- | --- | --- | --- | --- | --- |
| R1 | 4 | 14 | 12 | 30 | 文档从设计意向升级为执行文档 |
| R2 | 0 | 5 | 10 | 15 | 补齐中间类型、公式变量、选择算法 |
| R3 | 0 | 4 | 13 | 17 | YAML/Kotlin 结构不匹配 |
| R4 | 0 | 0 | 9 | 9 | 跨引用闭合、冗余消除、示例一致性 |
| R5 | 0 | 0 | 3 | 3 | 示例语义一致性收尾 |
| **累计** | **4** | **23** | **47** | **74** | |

**R5 结论**：

1. **无 P0、无 P1**，连续两轮零 P1。R5 的 3 个 P2 全部集中在示例间的语义一致性上，不涉及 Kotlin 骨架、数学公式或验证门槛。
2. R5 的 3 个 P2 问题本质上围绕同一叙事链（`underground_river → drowned_altar → forgotten_cache`）：
   - P2-R5-1 是 R4-P2-9 修复的副作用——把 `sunken_shrine` 改成 `forgotten_cache` 消除了孤立引用，但引入了循环引用。修复方式是从 `guaranteedContent` 中移除 `drowned_altar`。
   - P2-R5-2 是 R4-P2-4（terrainTagWeights 合并）的同类遗漏——roomTag 的合并规则需要同等处理。
   - P2-R5-3 是 R4-P2-2（discoveryRule 冗余）的语义延伸——`REVEAL_SECRET_ZONE` 与物理入口的关系需要一句话澄清。
3. **核心合同完全稳定**：Kotlin 骨架、数学公式、验证门槛、YAML ↔ Kotlin 一致性、跨文档一致性全部保持通过。

**放行建议不变**：Phase 4 文档体系已就绪，可进入 P4-W1 / P4-W3 并行实现。R5 的 3 个 P2 均为示例编辑，建议在提交代码前随手修正，不影响实现起步。

---

*审阅人：资深游戏设计与开发总监视角*
*审阅版本：Round 5*
*审阅状态：建议放行实现（与 R4 一致）*

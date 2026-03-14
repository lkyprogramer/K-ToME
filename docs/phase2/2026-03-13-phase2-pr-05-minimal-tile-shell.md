> 执行前必须先完整阅读并接受：
> `docs/phase2/2026-03-13-phase2-pr-04-snapshot-and-manifest.md`
> `docs/2026-03-13-core-systems-design-and-phase-supplements.md`

# Phase 2 - PR-05 Minimal Tile Shell

**阶段**: `Phase 2 / P2-W5`  
**优先级**: `P1`  
**前置条件**: `P2-W4` 完成  
**对应问题**: 在 snapshot 和 manifest 都落地前，Tile 不该上；但一旦这两项稳定，就必须尽快建立最小正式 UI/Tile 壳层，让 Phase 2 不再停留在抽象合同阶段。

---

## 1. 阶段目标

建立最小 TileRenderer、最小 HUD、最小背包/检视壳层，并完成首轮中英双语 UI 走查。

完成标准：

1. 地图、角色、交互物走 Tile 正式路径。
2. HUD、背包、检视最小壳层成立。
3. 中英双语在最小正式界面上可以稳定显示。
4. screenshot golden 扩到主要玩家界面。
5. UI 音频、输入反馈和 zone -> tileset 选择逻辑进入正式主路径。

## 2. 当前问题

1. 只有 manifest 还不够，必须有真实 Tile 路径验证规则边界。
2. 没有 HUD/背包/检视最小壳层，就无法验证 locale、资源、状态、物品展示链路。
3. 没有最小 UI 音频和 tileset 语义时，后续职业/zone 资源虽然能导入，但玩家体验仍是拼装态。

### 2.1 本 PR 必须冻结的口径

1. 正式玩家路径不再以 ASCII 为主。
2. UI 先做功能闭环，不追求最终美术 polish。
3. HUD/背包/检视必须都从 snapshot + key 渲染。
4. zone 与 tileset 的绑定必须从第一天就是数据驱动，不允许 client 手写 if/else。

## 3. 范围与非目标

### 3.1 范围

1. TileRenderer
2. 最小 HUD
3. 背包/检视最小壳层
4. 双语 UI 走查
5. screenshot golden 扩充
6. 最小 UI 音频与输入反馈
7. zone -> tilesetKey 渲染接线

### 3.2 非目标

1. 不在本 PR 做最终动画和特效系统。
2. 不在本 PR 做复杂 UI 框架重构。
3. 不在本 PR 追求完整 UX polish。
4. 不在本 PR 大规模补所有职业和 zone 资源，但必须支持后续切片内容接线。

## 4. 技术方案

### 4.1 TileRenderer

建议文件：

```text
client/src/main/kotlin/com/ktome/client/render/TileRenderer.kt
client/src/main/kotlin/com/ktome/client/render/TileLayerComposer.kt
```

冻结口径：

1. 图层至少分：
   - terrain
   - prop/interactable
   - actor
   - overlay
2. 渲染顺序稳定。
3. footprint 与 pivot 按 manifest 语义走。
4. `zone.tilesetKey` 必须成为 TileRenderer 的正式输入，而不是客户端常量。

首批必须支持的资产族：

1. `tile_ground`
2. `tile_wall`
3. `tile_decal`
4. `prop_interactable`
5. `actor_sprite`
6. `icon_skill`
7. `icon_status`
8. `icon_item`
9. `icon_damage_type`

### 4.2 最小 HUD

必须展示：

1. 生命
2. 主资源
3. 状态列
4. 目标或检视焦点
5. 日志入口
6. 最小技能栏或热键栏

冻结口径：

1. HUD 只展示当前最小必要语义。
2. 先不做最终布局复杂化。
3. 状态和技能都必须优先走 iconKey / visualKey，不依赖纯文本。
4. 主资源展示必须绑定 `ResourceType` 的语义名称、颜色和百分比，不允许把 `MANA / POSITIVE_ENERGY / ENERGY` 都退化成同一条无语义蓝条。

### 4.3 背包 / 检视最小壳层

必须展示：

1. 物品名称 key
2. 物品图标
3. 基础说明
4. 角色关键属性概览

冻结口径：

1. 内容来自 schema + i18n，不允许手写第二套展示字符串。
2. 没有正式图标时允许 fallback。

### 4.4 UI 音频与输入反馈

本 PR 必须接入最小 cue：

1. `ui_confirm`
2. `ui_cancel`
3. `ui_hover`
4. `footstep_*`
5. `interactable_open`
6. `melee_hit_light`
7. `spell_cast_basic`

冻结口径：

1. 所有 UI 和操作反馈都通过 `AudioRouter` 和 cue family 走，不允许界面代码直接播裸文件。
2. 同一类操作至少支持基础变体组。

### 4.5 Phase 2 首批视觉语义

本 PR 必须支持以下最小视觉语义：

1. zone 根据 `tilesetKey` 切换基础 Tile family
2. actor 根据 `visualKey` 切换职业主体
3. interactable 根据 `visualKey` 显示不同热点对象
4. skill/status/item 根据 `iconKey` 显示基本 icon
5. telegraph / Boss warning / zone effect overlay 只来自 snapshot，不允许直接读 runtime world

首批至少要接通：

1. `tileset.ruins`
2. `actor.vanguard`
3. `actor.arcanist`
4. `icon.skill.*`
5. `icon.status.*`
6. `icon.item.*`
7. overlay/Boss warning 的最小三通道反馈：
   - 地面高亮
   - 日志 warning
   - 音频 cue

## 5. 推荐改动面

### 5.1 `client`

1. Tile renderer
2. HUD
3. Inventory shell
4. Inspect shell
5. AudioRouter UI 接线

### 5.2 `game`

1. 补足最小界面需要的 key 和 schema 字段
2. zone -> tilesetKey 配置

## 6. 测试与自证

### 6.1 必测类

1. `TileRendererGoldenTest`
2. `HudSnapshotRenderTest`
3. `InventoryScreenGoldenTest`
4. `AudioRouterUiCueTest`

### 6.2 必测行为

1. Tile 图层稳定。
2. HUD 正确显示生命、资源、状态。
3. 背包和检视可用且支持双语。
4. screenshot golden 无非预期漂移。
5. UI/输入反馈能正确触发最小 cue。

### 6.3 自动化命令

```bash
./gradlew goldenScreenshot
./gradlew audioLint
./gradlew manifestLint
./gradlew :client:test
```

### 6.4 白盒验证

1. 启动游戏进入默认局。
2. 检查 Tile 地图、HUD、背包、检视。
3. 切换语言重复检查。
4. 预期：
   - Tile 为正式主路径
   - HUD 信息完整
   - 双语布局无明显爆版
5. 触发移动、交互、确认等输入，确认 UI/操作音频出现且不过载。
6. 触发一个 overlay 场景，确认 overlay 来源于 snapshot 而不是直接读 world。

## 7. 出口门禁

1. Tile 正式路径成立。
2. 最小 HUD、背包、检视都可用。
3. 首轮双语 UI 走查通过。
4. 主要界面 golden 建立。
5. zone -> tileset 与最小 UI 音频链路成立。

## 8. 风险与止损

1. 如果 UI 需要继续读取规则内部结构，优先补 snapshot 字段，不绕过边界。
2. 如果布局频繁抖动，先冻结最小布局，不继续堆功能。
3. 如果资源不足，允许 fallback，但不允许回退到 ASCII 正式路径。

## 9. 当前状态

1. 本文是 `P2-W5` 的 PR 级开发文档。
2. 该 PR 完成后，Phase 2 才会真正进入可演示的正式表现路径。

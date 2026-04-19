# Validation Mode

`Validation Mode` 的职责是提速 `Phase 4` 人工白盒验证，不是替代正式 harness，也不是第二套规则权威。

## Phase 4 Mapping

| `docs/phase4` 工作包 | Validation preset | 快速路径 / 关键 action | 必看证据 |
| --- | --- | --- | --- |
| `PR-02` mapgen 差异性 | `MAPGEN_DIFF` | `Restart Next Seed` 轮转固定 `20260401 ~ 20260405` 五个 seed | seed / zone / terrain tag / hidden entrance 差异 |
| `PR-03` hidden entrance + search | `HIDDEN_CONTENT` | `Travel to Search Anchor` -> 正式 `SearchAction` -> `Travel to Hidden Entrance` | `SearchAction` 日志、reveal 状态、return bridge 可达 |
| `PR-05` loot | `LOOT_LAB` | `Present Reward`、`Spawn Item` | inspect 描述、icon、音频、日志来源 |
| `PR-06` terrain / elite / boss | `TERRAIN_INTERACTION` / `ELITE_MUTATION` / `BOSS_VARIANT` | terrain override、spawn elite、travel to boss | terrain rule、mutation 来源、boss variant 读屏可见 |
| `PR-07` hidden event / secret zone | `HIDDEN_CONTENT` | `Travel to Secret Reward`、`Travel to Secret Return` | hidden / secret zone 的 log、inspect、route 可读 |
| `PR-09` content pack | `CONTENT_PACK` | sample pack 启动后沿 search anchor / hidden entrance / secret reward 路径进入 pack 内容 | active pack ids、`sample_flooded_relics.*` 可见内容、namespace 可读性 |

## Manual Boundaries

Validation Mode 不能替代以下人工判断：

1. `MAPGEN_DIFF` 的五个 seed 是否真的存在至少三类可感知差异。
2. `SearchAction`、hidden content、secret zone 的文本反馈是否可读。
3. Boss variant 是否只改变 mutation / loot / 表现，而不破坏 phase graph。
4. sample content pack 的内容是否真实在客户端里可见。

## Usage Notes

1. Validation setup 的 `CONTENT_PACK` preset 默认启用 `examples/content-packs/sample.flooded_relics`。
2. 局内 overlay 会显示 active pack ids、seed corpus 和 preset 对应的 `Phase 4` quick-path / evidence 提示。
3. `Restart Next Seed` 只在 preset 的固定 corpus 内轮转；当前 `MAPGEN_DIFF` 使用固定五个 seed。

# Phase 4 UI/UX Follow-ups

本文件只记录在 PR 实施过程中被明确降级的非阻塞事项。任何条目必须写清触发条件、owner、关闭 PR 和验证方式；不得把当前 PR 的 P0/P1 出口门禁挪到这里。

禁止把任一 PR 的 P0/P1 出口门禁降级到本文件。

同步规则：

1. 一旦出现 `WARN` 降级、deferred stub 延后、lint 覆盖不足、资源 fallback 临时开放、连续软断言超预算或人工白盒无法完成，必须在合并前新增表格条目，并删除“当前无 deferred follow-up”这句话。
2. 每个条目必须写清关闭 PR；不能用“后续处理”或“待定”替代。
3. 对应 PR 收口时必须删除已关闭条目，或把未关闭原因升级回阻塞项。

| 条目 | 触发条件 | owner | 关闭 PR | 验证方式 |
| --- | --- | --- | --- | --- |
| PR04 legacy keyword formal-surface coverage | `keywordRegistryLint` 对 `diminishing_returns / dispel / dot / penetration / power_save / single_target / sustain` 产出 `WARN`：这些是 PR04 未新增、未修改的历史核心 keyword，目前没有进入 `DescriptionModel`、`ExplainPaneModel`、`StatusPresentationModel.nameKey` 或 registry related-keyword formal surface。PR04 已把 status/telegraph formal surface 纳入覆盖，新增/修改面仍保持 unknown id `ERROR` 与 fixture coverage `BLOCKED`。 | `tools/lint` + `core/talent` | `phase4-uiux-pr05-telegraph-and-combat-decision-surface` | 在关闭 PR 中为上述 keyword 补正式 description surface 或删除过期定义，并运行 `./gradlew keywordRegistryLint localeLint contractLint maintainabilityLint`。 |

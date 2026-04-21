# Phase 4 UI/UX Resource Fallback Audit Template

当某个 PR 未生成正式图片或音频资源，但正式 UI 路径需要 fallback 时，PR description 必须包含本模板的等价内容。

| key | 请求面 | fallback-visualKey / fallback-audioCueId | fallback 行为 | 失效风险等级 | 补交付 unblock task | 关闭 PR / owner | 是否开放正式玩家路径 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| | `item / card / status / telegraph / combat` | | | `low / medium / high` | | | `yes / no` |

约束：

1. fallback 不得成为第二真源；正式表现仍以 manifest / token / locale / content schema 为准。
2. fallback 不得引用 raw asset path。
3. fallback key 必须被 smoke、golden 或人工白盒记录消费到。
4. 如果 fallback 会长期存在，必须在对应 PR 的出口门禁中说明原因。
5. `失效风险等级=high` 且 UI 路径依赖新 key 时，不得开放正式玩家路径；smoke/golden/人工白盒只能作为 fallback 可见性证据，不能替代正式资源落地。
6. 只有 fallback 复用既有 key，且不依赖新 key 启用新 UI 路径时，才允许在同 PR 证据齐全后临时开放。
7. 若无 fallback，PR description 必须写“全部真实资源已落地”。

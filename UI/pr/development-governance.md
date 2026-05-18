# Dark UI/UX PR Development Governance

本文是 `UI/pr/` 后续 dark UI/UX 开发的 repo-owned 执行合同。它复用 Phase4 v4 治理中的 Acceptance Matrix、Gate Budget、canonical artifact 和失败复盘纪律，但 gate surface 以 UI / resource / client 验证为准。

通用 PR 级 review 方法论以 [docs/review/rule/pr-level-review-standard.md](../../docs/review/rule/pr-level-review-standard.md) 为准；本文只收敛 dark UI/UX 系列的 owner 取值、gate ladder、canonical artifact 和白盒证据口径。

## 1. 目标

dark UI/UX PR 不再依赖人工记忆执行资源 gate、golden 和白盒流程。每个 PR 必须先把文档合同结构化成验收矩阵，再按 fast lane、resource gate、client evidence、最终 `verifyChanged` 串行闭环。

固定目标：

1. 每个 PR 的完成定义、资源范围、golden / manual evidence 都能追到 fast check、owner gate 或 canonical artifact。
2. 资源 PR 先证明 registry / sheet plan / manifest / coverage 可执行，再进入截图和人工验收。
3. `clientSmoke`、`goldenScreenshot`、resource lint 和 packaged app 白盒不互相替代。
4. raw sheet、contact sheet、manifest、coverage artifact、manual record 全部使用 repo-relative path。

## 2. Acceptance Matrix

每个 `UI/pr/dark-uiux-pr*.md` 必须包含 `## 0. 开发治理与验收矩阵`，字段固定：

| Field | Required | Meaning |
| --- | --- | --- |
| `requirementId` | yes | 稳定需求编号，例如 `UI03-M01` |
| `source` | yes | PR 文档章节、完成定义、资源范围或 evidence matrix |
| `owner` | yes | `client` / `assets` / `tools` / `docs`；明确声明为 gameplay/content bridge 的 PR 还可使用 `core` / `game` |
| `fastCheck` | yes for blocking behavior | focused client test、resource lint、schema/runtime focused test 或静态检查 |
| `ownerGate` | yes for owner behavior | `clientSmoke`、`goldenScreenshot`、dark resource lint、schema/runtime owner suite、packaged app 白盒或 `N/A` |
| `artifact` | yes for evidence | repo-relative canonical artifact path |
| `whitebox` | yes | `required` / `skipped` / `N/A` |

执行规则：

1. player-visible UI 改动不能只靠 manual record；必须有 golden、clientSmoke 或 focused client test。
2. 新增或替换资源不能只靠 raw PNG；必须有 registry、sheet plan、manifest 和 coverage artifact。
3. `whitebox=skipped` 必须写明原因、替代证据和剩余风险。
4. 不得出现 `TBD owner`、`TBD gate`、`TBD artifact`。

## 3. Gate Ladder

dark UI/UX PR 固定按以下顺序执行：

1. `acceptanceContractLint`
   - 检查 `UI/pr` 文档是否具备验收矩阵、Gate Budget、canonical artifact 和 failure rule。
2. fast lane
   - 运行 PR 文档声明的 focused client test、manifest resolver test 或 resource static lint。
3. resource gate
   - 资源 PR 运行 `assetLint styleLint manifestLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint`。
   - `owner-scope` coverage 必须显式传 `ownerPr`；PR-06 / PR-07 的 close gate 必须使用 `final-full`，不得传 `ownerPr` 缩小分母。
4. client evidence
   - 运行 `:client:clientSmoke` 和 `:client:goldenScreenshot`。
5. governance gate
   - 结构性 client PR、renderer 重排、public presentation model 或 gate wiring 必须运行 `maintainabilityLint`。
6. final closure
   - 最后运行 `verifyChanged`。
   - PR-07 或 package-facing UI 变更必须保留 packaged app 白盒记录；不能只用 debug client golden 替代。

## 4. Gate Budget

每个 PR 必须声明：

1. 预计触发的重型任务。
2. 触发原因。
3. resource / manifest / golden freshness 要求。
4. 最近耗时来源或 `build/verification/verify-changed/full-task-duration-summary.{json,md}` 的读取方式。

失败复盘阈值：

1. 同一重型 gate 失败超过 `2` 次，先补 focused test 或 resource lint 断言，再重跑。
2. 单轮验证超过 `90` 分钟，先检查是否把 golden / packaged app 当成调试循环。
3. 复盘必须回写到 PR 文档、review 或 PR 描述。

## 5. Canonical Artifact

dark UI/UX canonical artifact 包括：

1. `UI/sprite-sheets/sheet-plan.yaml`
2. `UI/sprite-sheets/key-registry.yaml`
3. `assets-src/image/contact-sheets/dark-v1/`
4. `assets-src/image/manifests/phase2-visual-manifest.json`
5. `client/src/main/resources/manifests/visual-manifest.json`
6. dark manifest coverage report
7. `client` golden output
8. `UI/manual-records/` 或对应 PR manual record

禁止把以下内容作为长期合同：

1. Codex CLI transient source 目录，例如 `<codex-generated-images-dir>`。
2. 本机绝对路径。
3. 未切分 raw PNG 的临时文件名。
4. 未确认的候选 contact sheet。
5. debug-only screenshot metadata。

## 6. Doc-Vs-Implementation Self-Audit

每个 PR 收口前必须逐条审计：

1. PR 文档完成定义是否已实现。
2. 每个 resource key 是否有 registry、sheet plan、manifest、consumer test 或 golden。
3. 每个 player-visible UI surface 是否有 focused test、clientSmoke、golden 或白盒证据。
4. old-style residue、fallback、rejected cell 是否在 coverage artifact 中解释。
5. 未执行项是否真实记录。

## 7. PR-00 To PR-07 Inheritance

1. PR-00 交付 style / pipeline / lint 合同，是后续 PR 的前置。
2. PR-01 到 PR-04 以 client presentation 和 golden 为主，不改变 gameplay rule。
3. PR-05 是资源与 manifest 主体的 owner-scope 收口；PR-06 是 full manifest 主体收口，必须严格执行 final-full coverage；PR-07 继续使用 final-full 做最终 packaged / golden / whitebox polish。
4. PR-07 是最终 packaged app、golden、whitebox polish 收口，不新增大范围资源合同。

## 8. Gameplay / Content Bridge PR Exception

`UI/pr` 下允许少量 bridge PR 承接 UI 发现的 gameplay/content 合同缺口，但必须显式声明为 `gameplay/content bridge`，且不能放宽本文件的 evidence 纪律。

Bridge PR 固定附加规则：

1. `core` / `game` owner 合法，但必须把 schema/runtime owner suite 放入 blocking gate，不能只跑 `clientSmoke` 或 `goldenScreenshot`。
2. `client` 只能消费 typed snapshot、description model 或 presentation DTO；不得在 renderer/presenter 中计算 gameplay rule。
3. 涉及 official data、schema、resolver、snapshot、content pack 或 lint 的改动，必须在 Acceptance Matrix 中分别列 owner、fastCheck、ownerGate 和 artifact。
4. 若 bridge PR 同时触碰 `core/game/client/tools`，必须在文档内拆成可独立验收的顺序 slice，并写清每个 slice 的 stop rule 与 rollback invariant。
5. Bridge PR 不改变 PR-01 到 PR-04 的通用性质；它是显式例外，不能作为后续 UI PR 任意改 gameplay rule 的先例。

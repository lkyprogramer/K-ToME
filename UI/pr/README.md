# K-ToME Dark UI/UX PR 级开发文档索引

本目录是 [UI/PLAN.md](../PLAN.md) 的 PR 级执行落点。上层文档保留总方案、风格合同、雪碧图合同和公共验证纪律；本目录按真实开发工作量拆成可单独建分支、评审、验证、回滚的 PR。

## 执行顺序

| 顺序 | PR 文档 | 优先级 | 工作量 | 目标 | 资源生成 |
| --- | --- | --- | --- | --- | --- |
| 0 | [PR-00 Style And Pipeline Contract](dark-uiux-pr00-style-and-pipeline-contract.md) | P0 | M | 冻结风格合同、sheet schema、prompt/切分/验收流程 | 不生成正式资源 |
| 1 | [PR-01 Client Shell Layout](dark-uiux-pr01-client-shell-layout.md) | P0 | L | 首页/主菜单、验证入口、三栏 + 底部 HUD 框架、token、renderer 拆分 | 不生成正式资源 |
| 1.1 | [PR-01-1 Client Viewport Renderer Overlay](dark-uiux-pr01-1-client-viewport-renderer-overlay.md) | P0 | XL | 玩家居中地图视口、TileRenderer orchestration 化、tooltip/modal overlay layer | 不生成正式资源 |
| 2 | [PR-02 UI Chrome Sprite Pilot](dark-uiux-pr02-ui-chrome-sprite-pilot.md) | P0 | L | 跑通 UI chrome/HUD/standalone screen chrome 第一批 sheet 到 manifest/golden | Round 1 |
| 2.1 | [PR-02-1 Demo Shell Foundation](dark-uiux-pr02-1-demo-shell-foundation.md) | P0 | XL | 在 PR-02 chrome 基础上搭建 demo-like 主 shell：icon rail、dominant map stage、right grid scaffold、bottom hero/action/log deck | Round 1B 必须生成 |
| 3 | [PR-03 Equipment Inventory Items And Shop](dark-uiux-pr03-equipment-inventory-items.md) | P0 | L | 在 PR-02-1 right panel 合同上接入真实装备/背包 item icon、铭文商店、quality、空态/tooltip；不得恢复 right-panel ground loot 或 fake placeholder | Round 7 部分 |
| 4 | [PR-04 Profession Tree UI](dark-uiux-pr04-profession-tree-ui.md) | P0 | L | ToME 式天赋分配面板、树状列表、技能图标、当前等级详情、主动槽 modal | 仅为唯一参考图新增 PR04 reference-crop 证据资源；正式全量 rebaseline 归 PR-06 |
| 4.1 | [PR-04-01 Playable Profession Passive Talents](dark-uiux-pr04-01-playable-profession-passive-talents.md) | P0 | XL | 6 个可玩职业每职业至少 2 个明确被动；被动不占主动槽，右侧详情展示当前/下一级收益，统一装备/天赋 passive resolver | 不生成正式资源 |
| 5 | [PR-05 Map Actor Portrait Replacement](dark-uiux-pr05-map-actor-portrait-replacement.md) | P1 | XL | Tile、prop、VFX、actor、portrait 统一替换 | Round 2-6 |
| 5.1 | [PR-05-1 Inventory Page Workbench](dark-uiux-pr05-1-inventory-page-workbench.md) | P1 | L | 全屏背包 workbench：9-slot 视觉装备区、6x4 背包 grid、typed detail/compare、键盘优先输入 | 不生成正式资源 |
| 6 | [PR-06 Skills Status Quest Full Manifest](dark-uiux-pr06-skills-status-quest-full-manifest.md) | P1 | XL | 技能、状态、任务、fallback、全 manifest 收口 | Round 8-9 + 返修 |
| 7 | [PR-07 Golden Whitebox Polish](dark-uiux-pr07-golden-whitebox-polish.md) | P1 | M | 全 UI 面 golden/白盒、验证模式、结算/错误页、性能与 atlas 决策 | 不新增资源，允许返修 |

## 依赖规则

1. 串行推进：`PR-00 -> PR-01 -> PR-01-1 -> PR-02 -> PR-02-1 -> PR-03 -> PR-04 -> PR-04-01 -> PR-05 -> PR-05-1 -> PR-06 -> PR-07`。
2. 每个 PR 必须先读 [UI/PLAN.md](../PLAN.md)、[UI/ART_STYLE_BIBLE.md](../ART_STYLE_BIBLE.md) 和本 PR 文档。
3. 每个 PR 完成后必须做一次 doc-vs-implementation self-audit。
4. 每个 PR 的 golden label 默认使用 `dark-uiux-prNN-*` 前缀；不复用旧 `phase4-uiux-prNN-*` label。`PR-01-1`、`PR-02-1`、`PR-05-1` 是合法细分特例，分别使用 `dark-uiux-pr01-1-*`、`dark-uiux-pr02-1-*` / 已冻结的 `ui-demo-new-*` 和 `dark-uiux-pr05-1-*`。`PR-02-1` 的 demo parity 主证据已经固定为 `ui-demo-new-*` label，路径和 manual record 仍归 `dark-uiux-pr02-1` owner；后续 PR 不得再引用旧 `dark-uiux-pr02-1-demo-*` label 作为必填 evidence。
5. 新增图片、manifest、sheet plan、contact sheet 或 runtime PNG 的 PR 必须补跑 `assetLint styleLint manifestLint`。
6. 新增或改中文 UI 文案、locale token、presentation token 的 PR 必须补跑 `localeLint contractLint`。
7. 修改 Kotlin 文件数 `>= 5`、新增 public presentation model、或重排 renderer 共享组件的 PR 必须补跑 `maintainabilityLint`。
8. 修改 Gradle、bootstrap、processResources、lint task 接线或依赖的 PR 必须补跑 `./scripts/verify-bootstrap.sh`。
9. PR-00 关闭前必须让 `verifyChanged` impact routing 命中 dark-v1 相关变更时触发 dark gate；PR-02 以后不得只依赖人工记忆执行资源 gate。
10. `ownerPr` 字符串固定使用 `PR-00`、`PR-02` 或 `PR-02-1` 这种格式；禁止混用 `pr02`、`PR02`。若新增细分 PR owner，必须同步更新 dark sprite pipeline 的 ownerPr regex 和脚本回归测试。`PR-05-1` 不生成正式资源，因此不新增 sprite `ownerPr`；若未来把 PR05-1 参考图资源化，必须另行更新 regex 和脚本回归测试。
11. 所有 PR 必须遵守 [development-governance.md](./development-governance.md)，并包含 `## 0. 开发治理与验收矩阵`。
12. `acceptanceContractLint` 是 dark UI/UX PR 的文档合同快路径；它只检查 PR 文档是否可执行，不替代 resource gate、golden、白盒或 `verifyChanged`。
13. Gate ladder 固定为 `acceptanceContractLint -> fast lane -> resource gate -> client evidence -> maintainabilityLint -> verifyChanged`；PR-07 追加 packaged app 白盒。
14. 同一重型 gate 失败超过 2 次或单轮验证超过 90 分钟时，必须先写复盘并补 focused test / resource lint，再继续重跑。
15. 所有玩家可见或验证可见 UI 面必须映射到 [screen-coverage-matrix.md](./screen-coverage-matrix.md)。PR-07 关闭前，矩阵中 `Required` / `Conditional` 面不得存在 `missing` 或无证据的 `partial`。

## 开发治理入口

dark UI/UX PR 的长期治理入口固定为 [development-governance.md](./development-governance.md)。

本 README 只维护 PR 索引、sheet ownership 和资源/验证入口摘要；Acceptance Matrix、Gate Budget、Canonical Artifact、Failure Rule 与 doc-vs-implementation self-audit 的长期条款只在 [development-governance.md](./development-governance.md) 维护。

## 全量 UI 面覆盖入口

全量界面覆盖清单固定为 [screen-coverage-matrix.md](./screen-coverage-matrix.md)。它把首页/主菜单、角色创建、继续游戏异常、验证模式 setup 与 overlay、局内 shell、loading/error、独立错误页、胜利/失败结算、装备背包、铭文商店、职业树、技能/状态/任务、战斗选择、Look/Inspect、世界路线、属性分配、reward/frontstage、地图资源、设置/无障碍和 desktop title 全部映射到 owner PR 与证据。

执行规则：

1. PR-01 必须先覆盖首页、验证入口、standalone screen token 和局内 shell；client ASCII fallback 不再作为验收面。
2. PR-02 必须让首页、验证 setup、结算页、错误页和 modal 可消费统一 chrome/control key。
3. PR-02-1 必须把局内主 shell 从 text-first 三栏提升为 demo-like 结构；PR-03/05/06 只在这个 shell 上替换资源和细分面板，不再重推主框架。
4. PR-03 必须把铭文商店、buy/sell、满槽替换 modal、价格/affordability marker 和购买失败反馈纳入装备/背包同一 UX family；pre-rendered disabled reason 只有在同 PR 新增 typed source 时才是 blocking 状态。
5. PR-07 必须输出 `dark-uiux-pr07-final-all-screens` evidence index，逐项引用矩阵中每个 Required/Conditional 面的 golden、manual record、focused test 和 packaged app evidence。

## SheetId Ownership

本表只定义 PR ownership，sheet type、capacity、grid 仍以 [UI/PLAN.md](../PLAN.md) 的 Sheet Inventory 为上游合同。任何新增、删除、改名必须同 PR 同步更新 `UI/PLAN.md`、本表和 `sheet-plan.yaml`。
本表是人类可读视图；机器真源是 `UI/sprite-sheets/sheet-plan.yaml` 与 `UI/sprite-sheets/key-registry.yaml`。lint 可以用结构化真源生成 ownership report 来校验本表，但不得依赖解析 Markdown 表格作为权威输入。

| Sheet ID | Owner PR | Cell Categories | Expected Key Prefix / Scope |
| --- | --- | --- | --- |
| `r01-ui-chrome` | PR-02 | `ui_frame` | `ui.frame.*`; standalone screens consume shared `ui.frame.panel.body` directly. Future exact `ui.screen.*` frame keys require later direct cells / manifest entries / tests. |
| `r01-ui-controls` | PR-02 | `icon` | `ui.control.*`, `ui.combat.*`, `ui.state.*`, `ui.screen.*` marker |
| `r01-ui-hud-icons` | PR-02 | `icon` | `ui.hud.*` |
| `r01b-ui-shell-chrome` | PR-02-1 | `ui_frame`, `icon` | Mandatory Round 1B shell scaffold keys: `ui.shell.*`, including nav rail icons, shell frame components, and map-stage backdrop |
| `r02-tiles-ground` | PR-05 | `tile_ground` | `tileset.<tilesetId>.ground_01` for `forest_edge`, `mine`, `ruins`, `shadow_depths` |
| `r02-tiles-wall` | PR-05 | `tile_wall` | `tileset.<tilesetId>.wall_01` for `forest_edge`, `mine`, `ruins`, `shadow_depths` |
| `r02-tiles-decal` | PR-05 | `tile_decal` | `vfx.terrain.interaction.*`; no new `tile.decal.*` runtime key |
| `r03-props-interactable` | PR-05 | `prop_interactable` | `prop.*` entries representing stairs, crate, gate, altar, ferry, reliquary or other interactable map props |
| `r03-props-environment` | PR-05 | `prop_environment` | decorative / environmental `prop.*`, excluding `zone.*.visual` |
| `r03-vfx-telegraph` | PR-05 | `vfx_plate`, `tile_decal` | `vfx_plate`: `vfx.boss.warning.*`, `vfx.telegraph.warning.*`, `vfx.boss.variant.*`; `tile_decal`: `vfx.zone.effect.*` when kept as `1x1` map decals |
| `r04-actors-player` | PR-05 | `actor_sprite` | `actor.player`, profession actors |
| `r04-actors-humanoid` | PR-05 | `actor_sprite` | bandit/orc/cultist/humanoid actors |
| `r04-actors-monster` | PR-05 | `actor_sprite` | beast/undead/abyssal/crystal/river/forge actors |
| `r04-actors-boss` | PR-05 | `actor_sprite` | `actor.boss.*`, `boss.*.visual` |
| `r05-bestiary-humanoid-icons` | PR-05 | `icon` | humanoid-family `icon.monster.*` |
| `r05-bestiary-creature-icons` | PR-05 | `icon` | creature-family `icon.monster.*` |
| `r05-boss-icons` | PR-05 | `icon` | `boss.*.icon` |
| `r06-portraits-classes` | PR-05 | `portrait` | `portrait.*` class keys |
| `r06-portraits-trees` | PR-05 | `portrait` | `tree.*` portrait keys |
| `r06-portraits-zones` | PR-05 | `portrait` | `zone.*.visual`, secret zone visuals |
| `r07-items-base` | PR-03 | `icon_item` | base equipment, starting items, potions, scrolls |
| `r07-items-unique-artifact` | PR-03 | `icon_item` | unique and artifact item icons |
| `r07-items-affix-material` | PR-03 | `icon` | affix marker, quality marker, craft material, `ui.shop.*` marker |
| `r08-skills-vanguard-berserker` | PR-06 | `icon_skill` | Vanguard and Berserker skills/talents |
| `r08-skills-templar-rogue` | PR-06 | `icon_skill` | Templar and Rogue skills/talents |
| `r08-skills-arcanist-spellblade` | PR-06 | `icon_skill` | Arcanist and Spellblade skills/talents |
| `r09-status-damage` | PR-06 | `icon_status`, `icon_damage_type` | status, mutation, damage type |
| `r09-quest-zone-profession` | PR-06 | `icon_quest`, `icon` | `icon.quest.*`, `zone.*.icon`, `icon.profession.*`, `icon.tree.*`, `difficulty.normal.icon` |
| `r09-fallback-debug` | PR-06 | `debug`, `icon`, `ui_frame` | missing, hidden, debug, fallback, locked/placeholder |
| `r09-rejected-polish` | PR-06 | original valid cell category per rejected source | PR-03/05/06 rejected-cell polish |

`Cell Categories` 列表示该 sheet 内 cell 允许出现的 category 集合，不是 sheet 自身的单一 category。`r09-rejected-polish` 的每个 cell 必须继承原 rejected cell 的合法 category，禁止在 `sheet-plan.yaml` 里写 `mixed` 这类占位 category。

## Manifest Entry Targets

每个 PR 关闭前必须输出本 PR 的 manifest coverage summary。数量是 planning target，最终以 canonical `assets-src/image/manifests/phase2-visual-manifest.json`、runtime `client/src/main/resources/manifests/visual-manifest.json` 和 `sheet-plan.yaml` 的实际 key inventory 为准，但偏离必须解释。

| PR | 目标覆盖范围 | 目标口径 |
| --- | --- | --- |
| PR-02 | UI chrome / HUD / controls / standalone screen chrome | Round 1 三张 sheet 的全部非 reserved cell，含首页/验证/结算/error/loading 共享 key |
| PR-02-1 | Demo shell foundation / mandatory shell chrome | 局内主 shell 结构、nav rail、right scaffold、bottom deck；Round 1B 必须覆盖 `ui.shell.*` owner-scope |
| PR-02-2 | ui-demo-new visual parity / shell demo evidence | `ui-demo-new-*` golden labels, demo map/actor/prop keys still consumed at runtime, and `dark-v1-manifest-coverage-pr02-2-owner-scope.json`; consumed by PR-06 final-full as upstream-covered entries |
| PR-03 | item / equipment / affix / material / shop | `item.*`, item quality marker, inventory-specific UI key, `ui.shop.price.*`, `ui.shop.inscription.marker`, `ui.shop.replacement.slot_marker`; `ui.shop.offer.frame` alias 到 PR-02 `ui.frame.panel.body`，`ui.shop.offer.disabled` 默认 deferred |
| PR-05 | tile / prop / VFX / actor / portrait | Round 2-6 全部 player-visible key |
| PR-06 | skill / talent / status / mutation / quest / profession / tree / fallback | Round 8-9、PR-03/05 rejected cell、allowed fallback/exclusion |

职业覆盖使用 `releasePlayable=[vanguard, arcanist, rogue, templar]`、`devPlayable=[berserker, spellblade]`、`excludedFrozen=[shadowblade, warden]`。PR-06 必须把 frozen 排除写入 coverage artifact，不能把它们显示成 missing。

## Visual Manifest Field Policy

canonical、runtime、example 与 content-pack visual manifest 必须使用同一套严格字段合同。`VisualManifestEntry` 的玩家表现字段只允许承载资源定位、分类、footprint、pivot、tags 和可选 `tintColorHex`；不得在任何新提交的 manifest 里重新引入 client ASCII fallback 所需的 `asciiGlyph`、`asciiColorHex` 或等价字段。

`tintColorHex` 只用于同一基础视觉资源的表现层 tint metadata，例如 boss variant / actor variant 的色调差异；格式必须为 `#RRGGBB`。它不能表达规则状态、locale 文案、资源路径 fallback 或 terrain/item 通用颜色系统；需要新图时仍必须走 key registry、sheet plan、canonical manifest、runtime manifest 和 resolver/test 闭环。

`manifestLint`、runtime loader strict decoding、content-pack fixture 和 sample pack 必须把未知字段当作错误处理。发现 canonical / runtime / fixture / sample-pack manifest 中存在 `asciiGlyph` / `asciiColorHex` 时，正确修复是删除字段并补 Tile visual key 或 `tintColorHex`，不是恢复旧 ASCII renderer。runtime loader 只允许对 manifest v1 输入做 decode-only legacy strip：在 strict model decode 前剥离这两个历史字段，其他未知字段仍 fail fast，manifest v2+ 不继承该例外。

## Key Registry Contract

PR-00 必须交付 `UI/sprite-sheets/key-registry.yaml` 作为 key registry 真源。新增 UI key、item key、skill key、status key、quest key 都必须能落到同一格式：

| Field | Rule |
| --- | --- |
| `targetKey` | 等于 canonical/runtime `visual-manifest.json.entries[].key` |
| `category` | 属于 asset pipeline 白名单 |
| `ownerPr` | 对应本目录 PR 编号 |
| `sheetId` | 对应 SheetId Ownership 表 |
| `fallbackKey` | 缺图时使用的 manifest key |
| `consumer` | 主要消费文件或 presenter |
| `consumerTest` | 至少一个 focused test 或 golden label |
| `aliasOf` | 只有真实复用同一图时允许 |

禁止 renderer 里新增未登记的裸字符串资源路径。确实需要新 key 时，必须同 PR 更新 `sheet-plan.yaml`、key registry、manifest entry 和 focused test。

PR-00 的 lint 合同必须让非 reserved cell 缺少 key registry 记录或缺少 `ownerPr / fallbackKey / consumer / consumerTest` 时 fail fast。`aliasOf` 只能用于真实复用同一图，不允许用它掩盖缺资源。`aliasOf` 的目标必须存在，不允许链式循环。

## Manifest Authority

1. canonical manifest：`assets-src/image/manifests/phase2-visual-manifest.json`。
2. runtime manifest：`client/src/main/resources/manifests/visual-manifest.json`，只能由 `syncPhase2Manifests` 生成或同步。
3. `sheet-plan.yaml.outputName` 必须同时匹配 canonical/runtime manifest 的 `rawOutputPath`。
4. 新 key 的最小闭环是：`key-registry -> sheet-plan -> canonical manifest -> syncPhase2Manifests -> runtime manifest -> resolver/test -> dark coverage artifact`。
5. 旧 `assetLint / styleLint / manifestLint` 是旧资源与 canonical/runtime 一致性回归门禁；dark-v1 是否完成玩家可见覆盖，以 `darkManifestCoverageLint` 和 coverage artifact 为准。
6. 新增 UI key 进入 canonical manifest 前，必须先进入 key registry 和 sheet plan；PR-00 必须扩展 `manifestLint` 桥接，使 registry/sheet-plan 覆盖的 dark-v1 key 不再被判定为“缺少 upstream spec”。
7. dark registry 只补充 upstream spec coverage，不替代 manifest `prefixRules`。新增 `ui.*` key 优先使用显式 canonical entry；只有需要动态 fallback 前缀时才扩展 `prefixRules`，并且 canonical/runtime `prefixRules` 仍必须完全一致。

`darkManifestCoverageLint` 使用三种模式：

| Mode | Owner | Exit rule |
| --- | --- | --- |
| `pr00-dry-run` | PR-00 | dry-run fixture 可解释 missing/pending，task 不得静默成功 |
| `owner-scope` | PR-02 / PR-02-1 / PR-03 / PR-05 | 当前 PR owner scope 必须完整，scope 外 pending 必须进入 artifact |
| `final-full` | PR-06 / PR-07 | `oldStylePlayerVisibleKeys=[]` 且 `pendingOrRejectedPlayerVisibleCells=[]` |

通用命令协议固定为：

```bash
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=pr00-dry-run
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-xx
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=final-full
```

PR close gate 和 `verifyChanged` 不使用裸 owner-scope 命令，必须使用固定 task 与独立 report：

```bash
./gradlew darkManifestCoveragePr00DryRun
./gradlew darkManifestCoveragePr02OwnerScope
./gradlew darkManifestCoveragePr02_1OwnerScope
```

`darkManifestCoverageLint` 裸 task 默认等价 `final-full`；CI、`verifyChanged` 和 PR close gate 必须显式传 mode。`owner-scope` 缺少 `ownerPr` 必须 fail fast；`final-full` 不允许 `ownerPr` 改变分母。PR-02 owner report 固定为 `dark-v1-manifest-coverage-pr02-owner-scope.json`，PR-02-1 owner report 固定为 `dark-v1-manifest-coverage-pr02-1-owner-scope.json`，不得复用裸 task 的 `dark-v1-manifest-coverage.json`。

coverage artifact schema 以 PR-00 文档为权威。README 只要求 common fields：`scopeMode / ownerPr / expectedKeySetSource / strictOldStyleResidue`。

PR-00 的 `verifyChanged` dark route 必须显式调用 `darkManifestCoveragePr00DryRun`。PR-00 fixture 允许 `missing_visual` 用来证明 manifest/resolver/coverage 链路；PR-02 / PR-02-1 / PR-03 / PR-05 必须用 `owner-scope` 逐步替换该 dry-run 口径，PR-06 / PR-07 必须用 `final-full` 收口。

HUD 与 item namespace 必须分开：

| UI 概念 | Namespace | Owner |
| --- | --- | --- |
| HUD 金币/钥匙/背包/装备提示 | `ui.hud.*` 或 `ui.control.*` | PR-02 |
| 背包中的金币/钥匙/物品实例 | `item.*.icon` 或既有 quest/item key | PR-03 |
| 任务钥匙 marker | `icon.quest.*` | PR-06 coverage |

同一张图可复用，但必须通过 `aliasOf` 显式声明，不能让 PR-02 与 PR-03 各自生成语义相同的 `gold/key` 图。

## Codex CLI Raw Sheet Workflow

雪碧图 raw sheet 统一由 `scripts/codex-generate-image.py` 调用 Codex CLI 生成。Codex CLI 当前会把图片写到 `<codex-generated-images-dir>/<latest-session>/`；仓库脚本按修改时间选择最新文件夹里的最新图片，并复制到 `sheet-plan.yaml.rawSheetPath`。

`<codex-generated-images-dir>` 只是 transient source，不允许进入 manifest、coverage artifact 或 PR 合同。正式资源路径必须是 repo-relative，例如 `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`。

固定流程：

1. 开发者更新 `UI/sprite-sheets/sheet-plan.yaml`。
2. 运行 PR-00 固定的 prompt 生成命令，输出 `UI/sprite-sheets/prompts/dark-v1/*.prompt.txt` 和 `prompt-index.json`。
3. prompt 文件按 `001-r01-ui-chrome.prompt.txt` 这种格式编号；编号只表示执行顺序，语义仍以 `sheetId` 为准。
4. 开发者运行 `scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report <buildReportPath> --overwrite`。
5. 脚本执行 `codex exec "<prompt>" --skip-git-repo-check`，从本次运行创建或触碰的 `<codex-generated-images-dir>` 目录取最新图片，并复制到 prompt 文件头指定的 `Expected output file`。
6. 输出文件名必须等于 `{sheetId}.png`，例如 `assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png`。
7. 开发者运行切分、contact sheet、QA 和 manifest/coverage 校验脚本。
8. contact sheet 被人工确认后，后续 PR 才能把切分后的 runtime PNG 和 manifest patch 作为可评审产物。

示例：

```bash
scripts/codex-generate-image.py "$(cat UI/sprite-sheets/prompts/dark-v1/001-r01-ui-chrome.prompt.txt)" \
  --out assets-src/image/raw/sheets/dark-v1/r01-ui-chrome.png \
  --smoke-report build/reports/verification/dark-uiux/codex-image-smoke-r01-ui-chrome.json \
  --timeout-seconds 300 \
  --overwrite
```

prompt 文件头必须包含：

| Field | Rule |
| --- | --- |
| `Prompt ID` | `{threeDigitOrder}-{sheetId}` |
| `Sheet ID` | 等于 `sheet-plan.yaml.sheets[].sheetId` |
| `Expected output file` | 等于 `sheet-plan.yaml.sheets[].rawSheetPath` |
| `Canvas` | 等于 sheet type canvas |
| `Grid` | 等于 sheet plan grid |
| `Cell` | 等于 sheet plan cell size |
| `Style tag` | `ktome-dark-fantasy-sprite-ui-v1` |

禁止事项：

1. 不手写 prompt 文件；只能由脚本从 `sheet-plan.yaml` 生成。
2. 不把 `<codex-generated-images-dir>` 中的 session id 或文件名当合同；必须由脚本复制成 `rawSheetPath`。
3. 不把多个候选 raw sheet 都放在正式 raw 目录；正式目录每个 `sheetId` 只能有一个 `{sheetId}.png`。
4. 不通过修改 `row/col` 适配生成错误；格子错位、串格、文字、水印、风格漂移时重跑 prompt。
5. 不让 CI 依赖本机绝对路径或 Codex CLI transient 输出目录。

## Evidence Matrix

每个 PR 必须保留最小证据表。路径可按实际实现调整，但必须 repo-relative，并在 PR 描述中列出。

| PR | Golden / Manual Evidence |
| --- | --- |
| PR-01 | `dark-uiux-pr01-home-main-menu`、`dark-uiux-pr01-home-new-run`、`dark-uiux-pr01-continue-unavailable`、`dark-uiux-pr01-validation-entry`、`dark-uiux-pr01-shell-1280x800`、`dark-uiux-pr01-shell-min-window`、`UI/manual-records/dark-uiux-pr01-shell.md` |
| PR-02 | `dark-uiux-pr02-round1-chrome`、`dark-uiux-pr02-hud-icons-pilot`、`dark-uiux-pr02-standalone-screen-chrome`、contact sheet QA、manifest diff |
| PR-02-1 | main labels：`ui-demo-new-parity-1672x941`、`ui-demo-new-parity-1280x800`、`ui-demo-new-right-panel-grid`、`ui-demo-new-bottom-deck-no-command-hints`、`ui-demo-new-inventory-page-1`、`ui-demo-new-inventory-page-2`；supporting crop labels：`ui-demo-new-nav-rail-crop`、`ui-demo-new-map-stage-crop`；manual：`UI/manual-records/dark-uiux-pr02-1-demo-shell-foundation.md`、`UI/manual-records/ui-demo-new-visual-parity.md`；coverage：`build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-1-owner-scope.json`、`build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-2-owner-scope.json` |
| PR-02-2 | `ui-demo-new-parity-*` labels and `build/reports/verification/dark-uiux/dark-v1-manifest-coverage-pr02-2-owner-scope.json`; source manual remains `UI/manual-records/ui-demo-new-visual-parity.md` |
| PR-03 | `dark-uiux-pr03-equipment-slots`、`dark-uiux-pr03-inventory-empty`、`dark-uiux-pr03-inventory-stacked`、`dark-uiux-pr03-inscription-shop`、`dark-uiux-pr03-shop-full-slot-replace`、`UI/manual-records/dark-uiux-pr03-fallback-key-injection.md` |
| PR-04 | `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png`、`dark-uiux-pr04-talent-assign-panel-start`、`dark-uiux-pr04-active-slot-choice`、`dark-uiux-pr04-talent-assign-min-window-log-visible`、`dark-uiux-pr04-right-companion-coexistence`、`UI/manual-records/dark-uiux-pr04-profession-tree-ui.md`、`phase4-v4-pr01` scenario evidence |
| PR-04-01 | `dark-uiux-pr04-01-static-passive-detail`、`dark-uiux-pr04-01-trigger-passive-detail`、`dark-uiux-pr04-01-passive-action-suppression`、`dark-uiux-pr04-01-effective-hp-regen-detail`、`UI/manual-records/dark-uiux-pr04-01-playable-profession-passive-talents.md`、`build/whitebox/dark-uiux-pr04-01-static-passive-detail/cua-runbook.md`、`build/whitebox/dark-uiux-pr04-01-trigger-passive-detail/cua-runbook.md`、`build/whitebox/dark-uiux-pr04-01-passive-action-suppression/cua-runbook.md`、`build/whitebox/dark-uiux-pr04-01-effective-hp-regen-detail/cua-runbook.md` |
| PR-05 | `dark-uiux-pr05-map-layer-stack`、`dark-uiux-pr05-actor-boss-telegraph`、contact sheet QA |
| PR-05-1 | design reference `UI/dark-uiux-pr03-inventory-page-reference.png` / `.prompt.txt`；golden labels `dark-uiux-pr05-1-inventory-workbench`、`dark-uiux-pr05-1-inventory-compare`、`dark-uiux-pr05-1-inventory-pagination`、`dark-uiux-pr05-1-inventory-min-window`；manual `UI/manual-records/dark-uiux-pr05-1-inventory-page-workbench.md` |
| PR-06 | `dark-uiux-pr06-status-quest-skill-overview`、`dark-uiux-pr06-talent-icon-rebaseline`、validation overlay coverage reference、manifest coverage artifact |
| PR-07 | packaged app command, runtime home, evidence dir, manual record, final doc-vs-implementation checklist, `dark-uiux-pr07-final-all-screens` evidence index |

## 职业树专用规则

职业树 UI 改造以 `docs/review/phase4/v4-pr/2026-04-24-phase4-v4-pr01-profession-tree-run-choice.md` 和实现中的 `TalentSidebarPresenter` 为上游合同：

1. 只改 client presentation、layout、资源映射和 golden。
2. 不改 `TalentProgression.learnableTalentIds`、starter 数、Tier 门槛、owner metric、long-run 分母。
3. 不把 `TalentTreeNodeSnapshot.category` 当字符串解析。
4. 数字键 `1-4` 只在 `ACTIVE_TALENT_SLOT_CHOICE` modal 内消费。
5. PR-04 必须按 `UI/dark-uiux-pr04-talent-assign-tree-icons-detail-reference.png` 尽量一比一还原 Talent Assign 面板；不得回退到 tier/branch grid 或完整装备/背包同屏参考。
6. 职业树图标正式全量重绘默认放到 PR-06；PR-04 只允许为 canonical Vanguard Talent Assign 首屏新增 reference-crop 证据 key，不能扩展成全职业资源重绘。

## 通用验证入口

所有 Gradle 命令前必须执行：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

开发内循环按 PR 文档列出的 focused tests 执行；PR close 前至少执行：

```bash
./gradlew :client:clientSmoke :client:goldenScreenshot verifyChanged
```

资源 PR 追加：

```bash
./gradlew assetLint styleLint manifestLint
```

暗黑雪碧图资源 PR 追加：

```bash
./gradlew darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-xx
./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=<pr00-dry-run|final-full>
```

`owner-scope` 必须显式传 `ownerPr`；`pr00-dry-run` 与 `final-full` 不得使用 `ownerPr` 改变分母。若 PR 文档声明了专属 sprite map report，例如 PR-03 的 `assets-src/image/manifests/dark-v1-pr03-sprite-map-report.jsonl`，`spriteSheetMapLint` 必须显式传对应 `-Pktome.darkUiux.spriteMapReport=...`，不得落回 PR-00 默认报告路径。

PR-00 必须把以下路径纳入 `verifyChanged` impact routing：`UI/sprite-sheets/**`、`assets-src/image/raw/sheets/dark-v1/**`、`assets-src/image/contact-sheets/dark-v1/**`、`client/src/main/resources/dark-v1/**`、`assets-src/image/manifests/phase2-visual-manifest.json`、`client/src/main/resources/manifests/visual-manifest.json`、`assets-src/image/manifests/dark-v1-*.json`、`assets-src/image/manifests/dark-v1-*.jsonl`。

UI 文案或 presentation contract PR 追加：

```bash
./gradlew localeLint contractLint
```

结构性 client PR 追加：

```bash
./gradlew maintainabilityLint
```

# Dark UI/UX PR-01-1 Deep Review Report Round 8

日期：2026-05-08

审查对象：`UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md`

审查口径：基于 round7 反馈后的当前文档版本，重新核对上一轮 5 个 P2 / 3 个 P3 是否被吸收，并继续按“最细小问题也标出”的标准审查 PR-01-1 对后续 PR-03/04/05/06/07 的长期 viewport、renderer、overlay、manual evidence 和 ASCII 删除验证合同。

本轮结论：**无 P0 / P1。Round7 的核心问题已被吸收；仍建议修 2 个 P2 和 4 个 P3。** 这些问题不会推翻 PR-01-1 的方案，但会影响验证覆盖完整性、manual record 可读性和脚本语义精确度。

已运行验证：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

结果：`BUILD SUCCESSFUL`。

已运行 ASCII 静态扫描：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii([._-]|$)' \
  client/src assets-src/image/manifests client/src/main/resources examples/content-packs \
  tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：退出码 `1`，即当前扫描路径内 0 命中。

补充扫描：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii([._-]|$)|\.ascii' \
  assets-src/image/specs assets-src/image/manifests client/src/main/resources/manifests \
  examples/content-packs tools/src/main/resources game/src/main/resources/data/tilesets
```

结果：0 命中。

## Findings

### P0

未发现。

### P1

未发现。

### P2

#### 1. ASCII 删除扫描未覆盖 `assets-src/image/specs/*.json`

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:291-293` 的正式扫描路径覆盖 `assets-src/image/manifests`，但不覆盖 `assets-src/image/specs`。
- `assets-src/image/specs/phase2-runtime-visual-specs.json` 在当前工作区实际删除了大量 `asciiGlyph` / `asciiColorHex` 和 `tileset.foundation.ascii.*` 条目，说明它属于本次 ASCII 删除的真实变更面。
- 我额外把 `assets-src/image/specs` 加入扫描后当前为 0 命中，但这不是文档 §9 的正式验证入口。

影响：

如果后续有人在 `phase2-runtime-visual-specs.json` 或同目录规格文件里重新加入 ASCII 字段，当前 PR-01-1 的正式扫描不会发现。考虑到该文件当前确实参与了本次删除，验证入口应覆盖同一变更面，否则 Deletion Checklist 与实际修改范围不一致。

修复方向：

把 `assets-src/image/specs` 加入 §9 的扫描路径，并在 §6 Deletion Checklist 中把 source spec 明确列为需要证明不存在 ASCII client fallback 字段的上游资源面。

#### 2. “每个 Must Test 的测试映射”被要求写进不匹配的 manual record 子段

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:54` 已要求 manual record 包含 `Verification Source`、`Frame Ownership Self-Audit`、`Overlay Conflict Evidence` 等子段。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:307` 要求在新增自动 lint 前，manual record 必须在 `Overlay Conflict Evidence` 或 `Frame Ownership Self-Audit` 中列出每个 Must Test 对应的测试类 / 方法名。
- Must Test 覆盖 viewport、renderer、overlay、evidence/compliance 多类内容，并不都属于 overlay conflict 或 frame ownership。

影响：

这会把 viewport deadzone、ASCII deletion、map-first 等测试映射塞进 overlay 或 frame ownership 小节，导致 manual record 结构语义混乱。后续 reviewer 需要找“某条 Must Test 对应哪个测试”时，反而要在两个不相关章节里搜。

修复方向：

把 `:307` 改成：

> manual record 必须在 `Verification Source` 子段列出每个 Must Test 对应的测试类 / 方法名；`Overlay Conflict Evidence` 只记录 overlay 冲突证据，`Frame Ownership Self-Audit` 只记录 frame 字段与 ownership。

如需要更强约束，也可以把 canonical required sections 增加 `Test Mapping`，但不要复用 overlay/frame 子段承载全量测试映射。

### P3

#### 1. `*.ascii*` 删除口径与 `.ascii([._-]|$)` 正则口径不完全一致

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:215` 写的是 `*.ascii*` client tileset key。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:291` 的扫描正则是 `\.ascii([._-]|$)`。

影响：

如果真实口径是“任何包含 `.ascii` 的 key 都禁止”，当前 regex 不会抓到 `.asciiFallback` 这种后缀。若真实口径只允许 dot/underscore/hyphen 分隔的 key family，则 deletion checklist 的 `*.ascii*` 说法过宽。

修复方向：

二选一即可：

- 保持当前 regex，把 checklist 改成 “`.ascii`、`.ascii.`、`.ascii_`、`.ascii-` key family”。
- 或保持 `*.ascii*` 口径，把 regex 简化为 `\.ascii`。

#### 2. `modalSafeBounds.footerTop` 与 “or equivalent bounds” 表述仍有轻微类型不一致

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:27` 说 `modalSafeBounds` 字段至少能表达 `leftContentEdge`、`rightContentEdge`、`headerBottom`、`footerTop` 或等价 bounds。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:140` 直接引用 `modalSafeBounds.footerTop`。

影响：

如果实现选择等价 rect，例如 `x/y/width/height` 或 `bottom/top`，文档又要求 `modalSafeBounds.footerTop`，review 时会出现“到底必须有 footerTop 字段，还是等价字段也可以”的小歧义。

修复方向：

要么把 `footerTop` 明确为必备字段；要么把 §5.9 改成 `modalSafeBounds.bottomSafeEdge` / `modalSafeBounds.bottom` 这类与 rect 表达兼容的中性说法。

#### 3. 新增 deadzone/jump invariants 未同步进入测试命名表

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:250-251` 新增了 even/odd deadzone 半开区间测试、per-axis jump snap 测试。
- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:307` 的测试命名表仍只有旧的 `centersSmallMapWithinBounds`、`keepsTopLeftInsideDeadzone`、`snapsBackToPlayerAfterInspect` 等名称，没有覆盖 half-open interval 和 per-axis jump threshold 的可反推命名。

影响：

这不是覆盖缺失，但会削弱文档自己要求的“测试命名必须能从失败名反推出合同”。失败名如果只叫 `keepsTopLeftInsideDeadzone`，不足以判断是 even/odd half-open 问题、还是普通 deadzone 内移动问题。

修复方向：

在 §9 命名表补 2-4 个明确名称，例如：

- `keepsDeadzoneHalfOpenForEvenAndOddCells`
- `snapsWhenHorizontalJumpExceedsThreshold`
- `snapsWhenVerticalJumpExceedsThreshold`
- `keepsDiagonalMoveWhenNeitherAxisExceedsThreshold`

#### 4. 验证块的 `set -e` 位于 SDKMAN 初始化之后

证据：

- `UI/pr/dark-uiux-pr01-1-client-viewport-renderer-overlay.md:286-289` 先 `source "$HOME/.sdkman/bin/sdkman-init.sh"`、`sdk env`，再 `set -e`。

影响：

如果开发者把整段作为脚本执行，SDKMAN 初始化或 `sdk env` 失败时，shell 可能继续执行后续 Gradle 命令，违背仓库“先修正环境，再分析构建或测试失败”的纪律。这个问题很小，但 PR-01-1 文档正在作为长期验证模板，建议把脚本语义写严。

修复方向：

把 `set -e` 移到第一行：

```bash
set -e
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
```

## Requirement Alignment

| Requirement | Evidence | Conclusion |
| --- | --- | --- |
| Round7 P2: `TileOverlayLayerTest` 进入 Acceptance Matrix | `UI/pr/...:43` 已列入 `UI01-1-M04` fastCheck | 一致 |
| Round7 P2: modal safe bounds typed authority | `UI/pr/...:27`, `:105`, `:140`, `:193`, `:202-205` 已引入 `modalSafeBounds` | 部分一致，仍有 `footerTop` vs equivalent bounds 小歧义 |
| Round7 P2: deadzone half-open interval | `UI/pr/...:89-90`, `:250`, `:350` 已定义半开区间与验证 | 一致 |
| Round7 P2: per-axis jump snap threshold | `UI/pr/...:92`, `:251` 已定义按轴判断 | 一致 |
| Round7 P2: ASCII scan error handling | `UI/pr/...:288-297`, `:305` 已区分 no-match / hit / command error | 部分一致，仍未覆盖 `assets-src/image/specs` |
| Round7 P3: `shellUsableContentArea` 分母 | `UI/pr/...:26`, `:81`, `:344` 已定义并引用 | 一致 |
| Round7 P3: 三条扫描命令残留 | `UI/pr/...:220` 已改为合并后的扫描命令 | 一致 |
| Round7 P3: `ModalStack.peek()` 命名 | 当前目标文档未再命中 `ModalStack.peek` / `peek(` | 一致 |

## 功能/系统一致性矩阵

| 系统/模块 | 文档设计摘要 | 当前实现状态 | 证据锚点 | 偏差说明 | 严重级别 |
| --- | --- | --- | --- | --- | --- |
| ASCII deletion validation | 删除 client ASCII renderer/model/manifest/evidence，并以扫描证明 | 文档部分一致 | `UI/pr/...:209-220`, `:291-305`; `assets-src/image/specs/phase2-runtime-visual-specs.json` diff | 正式扫描少了 specs 目录；regex 与 checklist 宽度略不一致 | Medium |
| Manual evidence mapping | manual record 记录 commands/evidence/frame/overlay/ASCII 等证据 | 文档部分一致 | `UI/pr/...:54`, `:307`, `:329` | “每个 Must Test 映射”应归 `Verification Source`，不应塞进 overlay/frame 小节 | Medium |
| Viewport deadzone | 半开区间、按轴 jump snap、manual evidence | 文档一致 | `UI/pr/...:89-92`, `:250-251`, `:350` | 仅测试命名表还可更精确 | Low |
| Modal overlay safe area | modal 使用 typed safe bounds，不由 renderer 二次计算 | 文档基本一致 | `UI/pr/...:27`, `:105`, `:140` | `footerTop` 字段名与 equivalent bounds 表述略有歧义 | Low |
| Current implementation readiness | fixed shell viewport / renderer split 尚待实现 | 未实现，符合当前文档阶段 | `FoundationGameScreen.kt`, `FoundationViewportSupportTest.kt` 当前仍是旧合同 | 这是后续实现任务，不是本轮文档阻塞 | Low |

## 玩法与体验审查

### 核心循环

Viewport deadzone 的核心合同已经比 round7 明确很多：半开区间、edge clamp、jump snap 都具备可测试公式。剩余测试命名问题不会改变体验，但会影响失败定位速度。

### 战斗体验

`COMBAT_DECISION`、targeting cursor、modal layer 与 bottom log 的关系已保持同一 overlay authority。`modalSafeBounds` 的字段命名若再收紧，可以避免战斗中弹层安全区实现分叉。

### 成长与构筑驱动

`PANEL_SLOT`、`PANEL_ROW_OR_CARD`、`MODAL_ROW` 已足够承接 PR-03 item/shop、PR-04 active-slot modal 和 PR-07 reward/frontstage。未发现会阻断长期装备/铭文/职业树改造的问题。

### 奖励驱动与掉落体验

reward/frontstage、world route、shop offer 已挂到同一 row/card anchor family，长期方向正确。后续 PR-07 只需引用该 family，不应再造 tooltip 坐标路径。

### 探索与新鲜感

map-first 的 `shellUsableContentArea` 分母已定义，探索视野占比不再是纯审美判断。需要在实现时保证 `GameShellLayout` 真的输出该 bounds，而不是只在文档存在。

### 新手体验与信息反馈

manual record 的证据结构还可以更清楚：把测试映射放入 `Verification Source`，把 overlay 冲突只放 overlay 小节，更利于新 reviewer 快速判断“这个 UI 行为到底由哪个测试锁住”。

### 系统耦合与体验断层

当前文档已基本避免 second-authority。剩余风险主要是验证面没有覆盖所有实际改动源，尤其是 `assets-src/image/specs`。

## 当前阶段必须解决的问题

1. 将 `assets-src/image/specs` 纳入 ASCII 删除扫描：当前 PR 实际修改了该目录，不能让正式验证入口漏掉它。
2. 将 Must Test -> test class/method 映射迁移到 `Verification Source` 或新增 `Test Mapping`：避免 manual record 结构语义混乱。
3. 对 `.ascii` 口径、`modalSafeBounds.footerTop`、新增测试命名表和 `set -e` 位置做低成本文档收口：这些都是实现前修最便宜，进入代码后容易变成 review 噪声。

## Removal/Iteration Plan

当前不新增删除计划。旧 `FoundationViewportSupport` snapshot-size 合同仍是后续代码实施时必须替换的路径，文档已在 Implementation Migration Locks 中约束。

## Additional Suggestions

1. 在 §9 ASCII scan 下方加一句：`assets-src/image/specs` 是 source spec 层，只要本 PR 删除过其中 ASCII 字段，就必须与 canonical/runtime manifest 一起扫描。
2. `Verification Source` 子段可以使用固定表格：`requirementId | Must Test item | test class | test method | artifact | status`。
3. 对 `modalSafeBounds` 建议用 rect 风格命名：`left/right/bottom/top`，再在说明里标注 `bottom == footerTop`，避免字段别名。

## Suggested Verification

已运行并通过：

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
./gradlew acceptanceContractLint
```

已运行并确认 0 命中的补充 ASCII scan：

```bash
rg -n -i 'AsciiRenderer|AsciiRenderModel|asciiGlyph|asciiColorHex|tileset_foundation_ascii|\.ascii([._-]|$)|\.ascii' \
  assets-src/image/specs assets-src/image/manifests client/src/main/resources/manifests \
  examples/content-packs tools/src/main/resources game/src/main/resources/data/tilesets
```

建议文档修正后再运行：

```bash
./gradlew acceptanceContractLint
```

## Summary

PR-01-1 当前文档已经足够作为长期 UI 改造底座，没有发现新的结构性阻塞。剩余问题主要是验证边界和证据组织：扫描路径要覆盖实际修改过的 source spec，Must Test 映射要进入正确 manual record 子段，少量术语/正则/脚本顺序需要再精确一层。修完这些后，文档层面可以进入实现阶段。

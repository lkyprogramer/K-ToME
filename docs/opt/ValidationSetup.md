# Validation Setup Owner 收口 + SessionCommand 分层拆分方案

## Summary

- `validation setup` 的内容发现、默认 sample-pack 可用性、以及 preset 所需 catalog，统一下沉到 `game` 的 typed setup model；`client/GameApp` 只消费 DTO，不再直接 new `DataLoader`，也不再解析 repo-root。
- 命令协议长期改成两层：`PlayerCommand` 只保留正式玩家动作，`Validation` 提升到更高层 `SessionCommand.Validation(...)`；`FoundationGameSession`、`CommandSource`、harness、audio 都转向 `SessionCommand`，不再被 `PlayerCommand.Validation` 污染。
- 采用你选定的“分阶段兼容迁移”落地：先引入新 owner / 新协议并迁走内部调用，再更新文档与测试，最后删掉 legacy 适配层。

## Key Changes

### 1. Validation setup 彻底回到 `game` owner

- 新增 `game.validation.ValidationSetupRequest`，输入只包含：
  - `locale`
  - validation profile / `PlayerCreationSelection`
  - `ValidationRuntimeSupport`
- 新增 `game.validation.ValidationSetupModel`，输出固定包含：
  - `playerCreationState`
  - `zones: List<ValidationZoneDescriptor>`
  - `bossVariantIds`
  - `samplePackAvailability: ValidationSamplePackAvailability`
- `ValidationZoneOption` 从 `client` 私有 UI 类型改为 `game.validation.ValidationZoneDescriptor` 或等价 game-owned DTO；`client` 只做展示映射。
- `GameModule.validationSetupModel(request)` 成为唯一 setup 数据入口。
  - 它内部只加载一次 schema catalog。
  - `playerCreationState`、zone 列表、boss variant 列表都从同一份 catalog 推导。
  - 不允许 `GameApp`/`ValidationSetupScreen` 直接碰 `DataLoader`。
- `GameApp.validationSetupContext(...)` 改成只拼接：
  - `GameModule.validationSetupModel(...)`
  - `validationLifecycle.refreshContinueAvailability()`
  - UI 本地状态
- `GameApp` 构造函数移除 `validationSamplePackSelectionProvider` 这种 repo-local provider。
- 新增 `ValidationRuntimeSupport`，由 launcher/bootstrap 注入，默认只表达 runtime 能力，不做内容发现。
  - `samplePackAvailability = Available(selection, displayName)`
  - 或 `Unavailable(reasonKey)`
- `DesktopLauncher` 成为 sample-pack 可用性的唯一解析 owner。
  - 解析顺序固定为：
    1. `ktome.validationSamplePackRoots`
    2. 应用相对目录 `validation-packs/sample.flooded_relics`（若存在）
    3. 否则 `Unavailable`
  - 明确禁止 repo-root fallback、`ktome.repo.root`、`examples/...` 这类开发目录假设进入运行时代码。
- `ValidationSetupScreen/Controller` 的 UI 行为固定为：
  - `CONTENT_PACK` preset 始终可见。
  - 当 `samplePackAvailability` 为 `Unavailable` 时，sample-pack toggle 只显示 unavailable 文案，不可开启。
  - 选择 `CONTENT_PACK` 并尝试 `Start` 时，返回明确 notice，不允许 silent fallback 到空 pack。
  - 其他 preset 不受影响。

### 2. 命令协议长期拆成 `SessionCommand`

- 新增顶层 `SessionCommand`。
- `PlayerCommand` 改为 `sealed interface PlayerCommand : SessionCommand`，但长期 end-state 不再包含 `Validation` 子类型。
- 新增正式 programmatic path：`SessionCommand.Validation(val action: ValidationAction)`。
- `CommandSource` 接口改成返回和回传 `SessionCommand`：
  - `nextCommand(snapshot): SessionCommand?`
  - `onCommandResult(..., command: SessionCommand, consumed: Boolean)`
- `InputHandler`、`ValidationCommandSource`、bot/harness command source 全部迁到 `SessionCommand`。
  - 普通输入仍直接产出 `PlayerCommand.*`
  - validation overlay 产出 `SessionCommand.Validation(...)`
- `FoundationGameSession.perform(SessionCommand)` 成为主入口。
  - `perform(PlayerCommand)` 仅作为兼容重载保留一个迁移窗口。
  - route/shop/pending-turn guard 改成基于 `SessionCommand` 判定。
  - validation dispatch 不再藏在 `when (PlayerCommand)` 主分支里。
- 新增中心化 `SessionCommandTraits` 或等价 owner，统一收口：
  - nominal turn consumption
  - route-selection / active-shop allowance
  - trace/render key
  - command channel（`PLAYER` / `VALIDATION`）
- `AudioRouter`、`ScenarioUtil.renderCommand`、`consumesTurn()`、harness trace/hash 渲染全部转用这个中心 helper，不再各自加 `Validation` 分支。
- `PlayerCommand.Validation` 的兼容策略固定为三步：
  1. Stage 1 保留为 deprecated adapter，只允许在兼容边界转成 `SessionCommand.Validation`
  2. Stage 2 所有内部调用、文档、测试迁到 `SessionCommand.Validation`
  3. Stage 3 删除 `PlayerCommand.Validation` 和所有 adapter

### 3. 文档与 contract 同步

- `docs/opt/cheatMode.md`、`docs/verification/validation-mode.md`、phase4 checklist 在 Stage 2 统一改写为：
  - “programmatic validation path = `SessionCommand.Validation(...)`”
  - 不再把 `PlayerCommand.Validation(...)` 当长期正式合同
- 文档补充 runtime support 规则：
  - validation sample pack 由 launcher/bootstrap 提供
  - 无 sample pack 时的 UI/行为是显式 unavailable，不是 fallback
- 如果需要过渡说明，只保留一次性 migration note，不能把 legacy path 写成长期双真源。

## Migration Stages

### Stage 1. 先收 owner，不改用户可见能力

- 落 `ValidationSetupModel + ValidationRuntimeSupport + GameModule.validationSetupModel(...)`
- `GameApp` 去掉 `DataLoader` 和 repo-root sample-pack 解析
- `DesktopLauncher` 新增 `ktome.validationSamplePackRoots` / app-relative pack 解析
- `ValidationSetupScreen` 加 unavailable UI 和 start rejection
- 同阶段引入 `SessionCommand`、`SessionCommand.Validation`、`SessionCommandTraits`
- 内部 command source / session / audio / harness 全部迁到 `SessionCommand`
- 保留 deprecated `PlayerCommand.Validation` adapter，仅用于兼容旧调用

### Stage 2. 文档与内部调用全面迁移

- 所有测试、smoke、harness、white-box tooling、review 文档改用 `SessionCommand.Validation`
- 移除新的 `when (PlayerCommand)` 中对 `Validation` 的依赖
- 确保 `PlayerCommand.Validation` 只剩 adapter 文件还引用

### Stage 3. 删除 legacy path

- 删除 `PlayerCommand.Validation`
- 删除 `perform(PlayerCommand.Validation...)` 兼容入口
- 删除所有 adapter 和过渡文档
- `PlayerCommand` 恢复为纯正式玩家动作协议

## Test Plan

- `GameModule` / validation setup:
  - `GameApp` 不再 import/use `DataLoader`
  - `GameModule.validationSetupModel(...)` 返回的 zones / boss variants / playerCreation 与当前 catalog 一致
  - `samplePackAvailability=Unavailable` 时，`CONTENT_PACK` preset 启动被明确拒绝
  - `ktome.validationSamplePackRoots` 提供有效 pack 时，`CONTENT_PACK` preset 正常启用并带上 active pack ids
- 命令协议:
  - `CommandSource` / `InputHandler` / `ValidationCommandSource` 全部产出 `SessionCommand`
  - `FoundationGameSession.perform(SessionCommand.Validation(...))` 等价于现有 validation 运行时行为
  - `PlayerCommand.Validation` adapter 在 Stage 1 仍可通，但内部会转换到 `SessionCommand.Validation`
  - `AudioRouter`、`ScenarioUtil.renderCommand`、`consumesTurn` 不再各自维护 validation 特判
- 回归:
  - `GameAppLifecycleTest`
  - `InputHandlerTest`
  - `ClientSmokeHarnessTest`
  - `ValidationSetupControllerTest`
  - `FoundationGameSessionTest`
  - `GameModuleTest`
  - `OfficialSliceStabilityTest`
  - `ZoneChainSmokeTest`
  - `maintainabilityLint`
- 额外 acceptance:
  - 搜索 `GameApp.kt` 不再出现 `DataLoader(` 或 `ktome.repo.root`
  - 搜索 `when (command: PlayerCommand)` 不再需要 validation 分支，legacy adapter 文件除外

## Assumptions

- Validation Mode 需要在“非仓库 checkout / 打包运行”下仍然有明确行为，因此 repo-root fallback 直接禁止。
- 不要求把 sample pack 强制内置进程序；默认策略是“launcher 显式提供或 app-relative 发现，否则 unavailable”。
- 本方案的长期正式 programmatic contract 是 `SessionCommand.Validation(...)`，不是 `PlayerCommand.Validation(...)`。
- 兼容迁移窗口只保留一个迁移 PR 序列；不能把 adapter 留成长期第二路径。

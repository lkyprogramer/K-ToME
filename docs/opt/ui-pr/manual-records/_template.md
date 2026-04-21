# Phase 4 UI/UX Manual White-Box Record Template

**PR**: `phase4-uiux-prNN`  
**记录人**:  
**复核人**: `N/A`  
**日期**:  
**结论**: `PASS / FAIL / BLOCKED`

## 1. 环境

| 项 | 值 |
| --- | --- |
| Git branch | |
| Git HEAD sha | |
| OS / JVM | |
| locale | |
| 窗口尺寸 | |
| seed / validation preset | |
| save slot / content pack | |
| 启动命令 | |
| JVM 参数 / feature flags | |

## 2. 输入序列

| # | 起始状态 / mode | 输入 | 预期行为 | 实际行为 | 结果 |
| --- | --- | --- | --- | --- | --- |
| 1 | | | | | |

## 3. 视觉与可读性检查

| 检查项 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| | | | | |

## 4. 错误 / 空态 / 回退检查

| 场景 | 预期行为 | 实际行为 | 证据路径 | 结果 |
| --- | --- | --- | --- | --- |
| | | | | |

## 5. 证据路径

| 类型 | 路径或说明 |
| --- | --- |
| screenshot | |
| screen recording | |
| golden hash | |
| smoke artifact | |
| copied payload | |
| log excerpt | |

要求：

1. screenshot / recording / artifact 必须给出 repo 内路径、CI artifact 路径或可追溯说明。
2. log excerpt 必须标注来源文件和行号范围；只粘贴文本不足以签收。
3. golden 被 skipped 时，本记录必须提供截图或录屏，并补齐 `Git HEAD sha / locale / seed / 窗口尺寸`。
4. 本记录文件必须随 PR 一并提交；不得只写在 PR comment 或本地临时文件。

## 6. 签收结论

1. 未通过项：
2. 需要回归的项：
3. 可进入下一 PR：`yes / no`

## 7. 双人签收

| 角色 | 姓名 / ID | 结论 | 备注 |
| --- | --- | --- | --- |
| 记录人 | | `PASS / FAIL / BLOCKED` | |
| 复核人 | | `PASS / FAIL / BLOCKED / N/A` | |

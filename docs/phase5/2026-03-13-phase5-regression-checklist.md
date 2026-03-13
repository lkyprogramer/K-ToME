# Phase 5 Regression Checklist

## 1. Automated Verification

```bash
./gradlew test
./gradlew :core:test
./gradlew tacticalAiHarness
./gradlew replayHarness
./gradlew perfSmoke
./gradlew soakRun
./gradlew localizationQa
./gradlew accessibilityQa
./gradlew packageRelease
```

### 必须检查的结果

1. tactical AI 固定场景回归稳定。
2. replay 可重放关键 run。
3. perf smoke 达到预算。
4. soak 无灾难性崩溃、无明显泄漏。
5. localization/accessibility QA 全绿。
6. 安装包可生成且可启动。

## 2. Fixed-Seed Manual Runs

### 2.1 Tactical Boss Run

1. 固定 seed、职业、Boss encounter。
2. 检查：
   - 走位
   - 技能时机
   - 仇恨切换
   - telegraph 响应

### 2.2 Replay / Death Report Run

1. 固定一次失败 run。
2. 检查：
   - replay 可重放
   - death analysis 能解释最近关键致死链

### 2.3 Soak Baseline Run

1. 固定 soak 配置。
2. 检查：
   - 内存曲线
   - atlas/audio 句柄
   - 异常与卡死

## 3. Manual White-Box Steps

1. 从安装包启动游戏。
2. 新开局，进入一场中后期 Boss 战，确认 AI 与 telegraph 表现符合预期。
3. 切换语言并检查关键 UI、日志、背包、天赋、结算页。
4. 调整字体或可读性选项，确认可访问性设置真实生效。

## 4. Reporting Template

每次封版回归必须记录：

1. build id
2. 平台与运行环境
3. seed / 职业 / 路线
4. tactical AI 观察结论
5. replay/death analysis 结论
6. perf/soak 结果
7. localization/accessibility 结果
8. 安装包验收结果

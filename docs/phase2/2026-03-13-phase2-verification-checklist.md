# Phase 2 Verification Checklist

## 1. Automated Verification

### 1.1 Core & Game

```bash
./gradlew test
./gradlew :core:test
./gradlew :game:test
./gradlew jacocoTestReport
./gradlew :core:jacocoTestCoverageVerification
```

### 1.2 Contract & Content

```bash
./gradlew localeLint
./gradlew contractLint
./gradlew goldenScreenshot
./gradlew soloClearLab
```

### 1.3 必须检查的结果

1. save/load 往返稳定。
2. locale key 无缺漏、无占位符不一致。
3. manifest 可完整解析。
4. screenshot golden 无非预期漂移。
5. 四职业短局实验室全部通过。

## 2. Manual White-Box Verification

### 2.1 首页与语言

1. 启动 client。
2. 在首页切换中文/英文。
3. 预期：
   - 菜单即时切换语言。
   - 新开局和读当前阶段存档都使用所选语言。

### 2.2 Tile 正式路径

1. 新开局进入默认 zone。
2. 预期：
   - 地图、角色、交互物全部按 Tile 渲染。
   - HUD、背包、检视和日志与当前 locale 一致。
   - 缺图/缺音时出现 fallback 和明确错误。

### 2.3 短局闭环

1. 用四个基础职业各开一局默认短局。
2. 预期：
   - 都能完成从开局到 Boss 或失败结算的闭环。
   - 不出现裸字符串、ASCII 正式路径回退、存档损坏。

## 3. Reproducibility Contract

1. 所有白盒验证都必须记录 seed、职业、locale、zone 链。
2. `golden screenshot` 必须固定分辨率、UI scale、字体包和 locale。
3. `SoloClearLab` 必须固定 scenario、输入脚本和胜负口径。
4. 验证失败时必须保留：
   - failing seed
   - snapshot/hash
   - 日志 token 输出
   - screenshot 差异

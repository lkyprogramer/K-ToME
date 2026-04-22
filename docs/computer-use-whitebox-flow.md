# Computer Use White-Box Flow

本文档定义 agent 使用 packaged app + Computer Use 做白盒验证的通用流程。具体路径、seed、preset、菜单项、输入序列和证据文件名，必须来自当前任务文档、PR 文档或人工记录模板，不在本文档写死。

## 1. 读取验证真源

开始前先确认当前任务的验证真源：

1. PR / phase / feature 文档中的白盒要求
2. 对应 manual record 模板或已有记录
3. 需要验证的 app bundle、窗口、入口菜单、preset、seed、输入序列
4. 需要保存的截图、日志、录屏、文本记录或复制 payload

如果文档没有规定 seed、preset 或输入序列，agent 应选择一个可复现值，并写入本次人工记录。

## 2. 打包

进入仓库根目录，使用仓库声明的工具链环境，再执行当前文档要求的打包任务。

```bash
cd <repo-root>

source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env

./gradlew <package-task>
```

K-ToME macOS app 当前常用打包任务为：

```bash
./gradlew :client:packageMacApp
```

打包完成后，从任务文档或构建输出确认实际 app bundle 路径，并在人工记录里写明。

## 3. 准备隔离运行目录

每次白盒验证应使用独立运行目录，避免污染本机已有存档、设置或历史资源。

```bash
cd <repo-root>

mkdir -p <whitebox-root>/evidence
mkdir -p <whitebox-root>/runtime-home
```

推荐约定：

```text
<whitebox-root> = <repo-root>/build/whitebox/<task-or-pr-id>
```

但最终路径应以当前任务记录为准。

## 4. 启动 packaged app

使用 packaged app 的真实可执行文件启动，不用 IDE、Gradle run 或测试 harness 代替。

前台启动模板：

```bash
JAVA_TOOL_OPTIONS="-Duser.home=<whitebox-root>/runtime-home" \
<app-bundle>/Contents/MacOS/<app-executable>
```

后台启动并保存日志模板：

```bash
JAVA_TOOL_OPTIONS="-Duser.home=<whitebox-root>/runtime-home" \
<app-bundle>/Contents/MacOS/<app-executable> \
> <whitebox-root>/evidence/app.log 2>&1 &

echo $! > <whitebox-root>/evidence/app.pid
```

进程定位模板：

```bash
pgrep -fl "<app-executable-or-bundle-name>"
```

结束进程模板：

```bash
kill "$(cat <whitebox-root>/evidence/app.pid)"
```

如果没有 pid 文件，再使用 `pgrep -fl` 确认目标进程后结束，不要误杀无关进程。

## 5. 连接 Computer Use

Computer Use 目标 app 使用当前任务给出的 bundle id 或启动后识别到的 app id。

```text
App=com.ktome.client
```

连接后确认：

1. 当前窗口属于刚启动的 packaged app
2. 窗口尺寸、locale、content pack、save slot 等前置条件符合当前任务
3. 当前画面与人工记录的起始状态一致

## 6. 执行 CUA 输入序列

按当前任务文档执行输入序列，不在 agent 运行时临时发明验收路径。

记录时至少保留：

1. 起始状态 / mode
2. 输入动作
3. 预期行为
4. 实际行为
5. 结果
6. 证据路径

如果需要 seed、preset、save slot 或特殊开关，应在执行前写入记录；如果实际操作中发生偏移，应记录偏移点和恢复方式。

## 7. 保存证据

证据路径由当前任务决定，推荐放在：

```text
<whitebox-root>/evidence/
```

文本记录模板：

```text
Time:
App:
Build:
Runtime home:
Scenario:
Preset:
Seed:
Start state:
CUA steps:
Observed result:
Evidence:
Known limitations:
```

截图和录屏优先使用 Computer Use 自身能力或当前任务规定的方式保存。禁止把裸 macOS `screencapture -x <file>` 当作 CUA 画面证据；它可能抓到真实桌面前台窗口，而不是目标 app 窗口。

如果 Computer Use 当前不能直接持久化截图文件，使用仓库脚本捕获目标 app 的真实 macOS 窗口。该脚本先用 CoreGraphics 枚举可见窗口并解析 window id，再只对该 window id 执行截图，同时生成 metadata 与 SHA-256 sidecar。

```bash
scripts/capture-macos-app-window.sh \
  --bundle-id com.ktome.client \
  --app-name K-ToME \
  --out <whitebox-root>/evidence/<scenario-step>.png
```

默认截图会按证据阅读用途压缩：输出 PNG 最宽 `1600px`、最高 `1200px`，并使用 PNG8 palette + strip 非必要 metadata。若某个细节必须保留 Retina 原始像素，显式加 `--raw`；若需要保留完整色彩但仍缩放压缩，使用 `--truecolor`；若需要更小的沟通用图，可以调低上限。

```bash
scripts/capture-macos-app-window.sh \
  --bundle-id com.ktome.client \
  --app-name K-ToME \
  --max-width 1280 \
  --max-height 900 \
  --out <whitebox-root>/evidence/<scenario-step>.png
```

每张截图证据必须同时记录：

1. PNG 文件路径
2. `<png>.metadata.txt`
3. `<png>.sha256`
4. 对应 CUA step / scenario

只有 `capture_mode=macos-window-id` 且 metadata 中的 `window_owner` / `window_pid` / `window_bounds` 对应目标 packaged app 时，才可作为 target-window screenshot evidence。

## 8. 更新人工记录

完成后把结果同步到当前任务对应的 manual record。

```text
<manual-record-path>
```

记录必须说明：

1. 实际启动的是哪个 packaged app
2. 使用的隔离运行目录
3. Computer Use 目标 app id
4. seed、preset、输入序列
5. 证据路径
6. 通过、失败或受限通过的原因

失败时保留失败现场证据，不要只写“未通过”。

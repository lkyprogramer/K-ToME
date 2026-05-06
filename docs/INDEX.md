# K-ToME Docs Index

## 1. 文档优先级

从高到低：

1. [2026-03-13-phase2-to-phase5-final-roadmap.md](./2026-03-13-phase2-to-phase5-final-roadmap.md)
   - 阶段划分、工作包、预算、出口门禁的执行权威
2. [2026-03-13-core-systems-design-and-phase-supplements.md](./2026-03-13-core-systems-design-and-phase-supplements.md)
   - 公式、数据结构、系统内部规则与阶段补充的设计权威
3. [2026-04-04-unified-white-box-verification-framework.md](./2026-04-04-unified-white-box-verification-framework.md)
   - 统一白盒验证框架、AI 消费合同、artifact/report 合同与人工一致性策略的设计权威
4. `docs/phase2/*` `docs/phase3/*` `docs/phase4/*` `docs/phase5/*`
   - 各阶段的执行说明、PR/工作包文档与 checklist
5. [2026-03-13-phase2-5-review-and-recommendations.md](./2026-03-13-phase2-5-review-and-recommendations.md)
   - 审阅参考与缺口说明，不直接覆盖路线图
6. [K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md](./K-ToME_Phase2_to_Phase5_PR_Development_Guide_v2_SinglePlayer_Tile_i18n.md)
   - 历史参考，旧 `PR01 ~ PR12` 编号只作背景材料
7. [Roguelike 游戏开发指导文档.md](./Roguelike%20游戏开发指导文档.md)
   - 技术参考和算法思路来源
8. [2026-03-13-art-style-bible.md](./2026-03-13-art-style-bible.md)
   - 美术风格、style tag 和图片生成约束的权威参考
9. [project-architecture-mermaid.md](./project-architecture-mermaid.md)
   - 当前仓库静态架构总览，帮助快速理解 `core / game / client / tools`、official data、pack 与 report 的关系
   - draw.io 源文件：[project-architecture-drawio.drawio](./project-architecture-drawio.drawio)
   - 导出预览：[project-architecture-drawio.png](./project-architecture-drawio.png)
10. [project-functional-flow-mermaid.md](./project-functional-flow-mermaid.md)
   - 当前主线的动态流程图，覆盖启动、装载、建局、回合循环、隐藏内容、报告与 Phase 5 衔接
   - draw.io 源文件：[project-functional-flow-drawio.drawio](./project-functional-flow-drawio.drawio)
   - 导出预览：[project-functional-flow-drawio.png](./project-functional-flow-drawio.png)

## 2. 执行编号

1. Phase 2 统一使用 `P2-W1 ~ P2-W7`
2. Phase 3 统一使用 `P3-W1 ~ P3-W6`
3. Phase 4 统一使用 `P4-W1 ~ P4-W5`
4. Phase 5 统一使用 `P5-W1 ~ P5-W5`
5. 旧 `PR01 ~ PR12` 不再作为执行编号

## 3. 常见查找路径

1. 想看当前应该做什么：
   - 先看路线图
2. 想看某个系统的公式和结构：
   - 看补充设计文档
3. 想看某个阶段怎么拆任务：
   - 先看对应 `phaseN/roadmap.md` 和该阶段主文档
   - Phase 2 入口： [phase2/roadmap.md](./phase2/roadmap.md)
   - Phase 4 content pack schema / schema v1 -> v2 迁移入口： [phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md#453-版本兼容策略](./phase4/2026-03-13-phase4-procgen-loot-and-content-pack.md#453-版本兼容策略)
4. 想看某个工作包如何验收：
   - 看对应阶段 checklist
5. 想看统一白盒验证、AI 如何消费报告、以及自动化白盒如何与人工体验对齐：
   - 看 [2026-04-04-unified-white-box-verification-framework.md](./2026-04-04-unified-white-box-verification-framework.md)
6. 想看 verification 基础设施和 task perf monitor：
   - shared automation defaults、task perf boundary 与入口说明先看 [verification/README.md](./verification/README.md)
   - task perf monitor 细节见 [verification/test-task-performance-monitoring.md](./verification/test-task-performance-monitoring.md)
7. 想快速理解项目全局结构与主流程：
   - 看 [project-architecture-mermaid.md](./project-architecture-mermaid.md)
   - 需要可编辑源图时，看 [project-architecture-drawio.drawio](./project-architecture-drawio.drawio)
   - 看 [project-functional-flow-mermaid.md](./project-functional-flow-mermaid.md)
   - 需要可编辑源图时，看 [project-functional-flow-drawio.drawio](./project-functional-flow-drawio.drawio)

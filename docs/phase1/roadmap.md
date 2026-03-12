# Phase 1 Roadmap（入口）

**更新时间**: 2026-03-12  
**主入口**: `docs/phase1/2026-03-12-phase1-roadmap.md`

---

## 1. Phase 1 主题

1. Phase 1 的目标不是先把 ToME 全量系统铺开，而是先交付一个可以从开局打到通关的、可自证的 MVP。
2. 执行顺序固定为：
   - `1-1.0` 引擎基础 + 可验证
   - `1-2.0` 战斗与 AI
   - `1-3.0` 物品与天赋
   - `1-4.0` 完整循环
   - `1-5.0` 打磨与发布
3. 每个子阶段都必须同时交付四类产物：
   - 可运行切片
   - 对应核心单元测试
   - JaCoCo 覆盖率证据
   - 人工白盒验收清单
4. `core` 必须始终保持零引擎依赖，所有游戏规则、公式、AI、地图、存档都必须能在 JVM 单元测试里自证。
5. 从 `1-1.0` 开始就要建立 `./gradlew test` 与 `:core:jacocoTestCoverageVerification` 的失败门禁，不能把“可自证”拖到收尾阶段再补。

---

## 2. 文档索引

1. `docs/phase1/2026-03-12-phase1-roadmap.md`
2. `docs/phase1/2026-03-12-phase1-1.0-foundation-and-verifiable-core.md`
3. `docs/phase1/2026-03-12-phase1-2.0-combat-and-ai.md`
4. `docs/phase1/2026-03-12-phase1-3.0-items-and-talents.md`
5. `docs/phase1/2026-03-12-phase1-4.0-full-game-loop.md`
6. `docs/phase1/2026-03-12-phase1-5.0-polish-and-release.md`

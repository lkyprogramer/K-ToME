# K-ToME 项目架构图

本文档提供仓库当前基线下的静态架构视图。重点不是展示某个单文件调用链，而是固定：

1. 权威文档与冻结合同如何约束实现。
2. `core / game / client / tools` 如何分层。
3. 官方 data、content pack、asset manifest 与 white-box/report 如何接入主路径。
4. 当前 `Phase 4` 和下一阶段 `Phase 5` 的落点在哪里。

```mermaid
flowchart TB
    subgraph Governance["规划与合同层"]
        roadmap["长期路线图<br/>Phase 2~5 Roadmap"]
        phaseDocs["阶段入口与 PR 文档<br/>docs/phase2 ~ docs/phase5"]
        checklists["Verification Checklist<br/>white-box framework / phase gates"]
        frozen["冻结合同<br/>DamageType / ResourcePool / RenderSnapshot / LootBudget / ContentPackManifest"]
    end

    subgraph Inputs["内容与资源输入层"]
        official["官方内容数据<br/>game/src/main/resources/data"]
        samplePack["示例 Content Pack<br/>examples/content-packs/sample.flooded_relics"]
        fixturePack["测试夹具 Pack<br/>tools/.../fixtures/content-packs"]
        assets["资源规格与 Manifest<br/>assets-src / client manifests / i18n bundles"]
    end

    subgraph Runtime["运行时模块层"]
        core["core<br/>确定性规则真源<br/>ECS / Combat / Status / AI / ProcGen / Loot / Save / Replay DTO"]
        game["game<br/>Schema / Registry / DataLoader / FoundationGameSession<br/>官方内容与 Pack Overlay 装配"]
        client["client<br/>Desktop Entry / Input / Tile UI / HUD / Audio<br/>RenderSnapshot 消费"]
        tools["tools<br/>Lint / Smoke / Harness / Lab / White-Box / Phase Report"]
    end

    subgraph Phase4["当前 Phase 4 能力域"]
        world["世界与路线<br/>WorldGraph / Quest / Gate / RouteReward"]
        mapgen["地图与可解性<br/>MapgenPipeline / Biome / Vault / TerrainTag / SolvabilityGraph / SearchAction"]
        loot["掉落与遭遇生态<br/>LootBudget / Affix / Unique / Artifact / Elite Mutation / Boss Variant"]
        hidden["隐藏内容<br/>HiddenEvent / SecretZone / ReturnBridge"]
        packs["扩展点<br/>ContentPackManifest / Overlay / Diagnostics / Precedence"]
    end

    subgraph Outputs["输出与验收层"]
        runtimeOut["运行时输出<br/>GameEvent / LogTokenEvent / RenderSnapshot / Save / Replay"]
        reports["验证输出<br/>tools/build/reports/*<br/>white-box artifacts / phase4 summary"]
        phase5["Phase 5 Next<br/>Tactical AI / Replay Harness / Perf & Soak / QA / PackageRelease"]
    end

    roadmap --> phaseDocs
    phaseDocs --> checklists
    checklists --> frozen

    frozen --> core
    official --> game
    samplePack --> game
    fixturePack --> tools
    assets --> game
    assets --> client

    game --> core
    client --> game
    tools --> core
    tools --> game
    tools --> client

    core --> world
    core --> mapgen
    core --> loot
    core --> hidden

    game --> world
    game --> mapgen
    game --> loot
    game --> hidden
    game --> packs

    core --> runtimeOut
    game --> runtimeOut
    client --> runtimeOut

    mapgen --> reports
    loot --> reports
    hidden --> reports
    packs --> reports
    tools --> reports

    runtimeOut --> phase5
    reports --> phase5
```

## 读图要点

1. `core` 仍是唯一规则真源，不能被 `game`、`client` 或 `tools` 反向污染。
2. `game` 的职责不是重新定义规则，而是把官方内容和 content pack 装配到 `core` 的 typed contract 上。
3. `client` 只消费 `RenderSnapshot`、事件和 manifest，不直接决定战斗、掉落、AI 或地图规则。
4. `tools` 不是附属脚本目录，而是正式门禁 owner；`phase4Report`、white-box 与 harness 都在这里收口。
5. `Phase 4` 的核心不是单个功能，而是把 `ProcGen + Loot + Hidden Content + Content Pack` 统一接到一套稳定合同与报告路径上。
6. `Phase 5` 会直接消费当前 runtime output 和 verification output，而不是再造第二套 trace 或 QA 语义。

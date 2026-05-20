# Dark UI/UX PR05 Map Actor Portrait Replacement Manual Record

## Required Fields

| Field | Value |
| --- | --- |
| label | `dark-uiux-pr05-map-layer-stack`, `dark-uiux-pr05-actor-boss-telegraph` |
| scenarioId | `dark-uiux-pr05-map-layer-stack`, `dark-uiux-pr05-actor-boss-telegraph` |
| seed | `202605090501`, `202605090502` |
| viewport | `1280x800` |
| locale | `zh-CN` |
| ownerPr | `PR-05` |
| requiredOwnerSheetIds | `r02-tiles-ground`, `r02-tiles-wall`, `r02-tiles-decal`, `r03-props-interactable`, `r03-props-environment`, `r03-vfx-telegraph`, `r04-actors-player`, `r04-actors-humanoid`, `r04-actors-monster`, `r04-actors-boss`, `r05-bestiary-humanoid-icons`, `r05-bestiary-creature-icons`, `r05-boss-icons`, `r06-portraits-classes`, `r06-portraits-trees`, `r06-portraits-zones` |
| coverageReportPath | `build/reports/verification/dark-uiux/dark-v1-manifest-coverage.json` |
| spriteMapReportPath | `assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl` |
| contactSheetPaths | see `Contact Sheets` |
| goldenArtifactPath | `client/build/reports/golden/dark-uiux-pr05/evidence-index.tsv` |
| rawSheetSourceTrace | see `Raw Sheet Source Trace` |
| result | `PASS_AUTOMATED_GATES_WHITEBOX_SKIPPED_BY_REQUEST` |
| knownLimitations | Manual packaged whitebox / Computer Use validation is intentionally not claimed in this run per user instruction. Automated resource lint, owner-scope coverage, client focused tests, client smoke, and golden screenshot evidence are the acceptance evidence for this pass. |

## Resource Generation

Every PR05 raw sheet was generated through the repository Codex CLI wrapper:

```bash
python3 scripts/codex-generate-image.py "$(cat <promptPath>)" --out <rawSheetPath> --smoke-report <buildReportPath> --overwrite
```

The generated source folders are transient Codex CLI outputs. This record only stores repo-relative prompt/raw/contact paths and source summary labels, not machine absolute paths.

After initial contact review, several Codex raw sheets had merged or cross-cell subjects. They were not accepted directly. The accepted raw sheets were repacked into the fixed `sheet-plan.yaml` grid, contact sheets were regenerated, runtime PNGs were sliced from the accepted raw sheets, and `spriteSheetMapLint` rewrote the PR05 sprite map report from those accepted artifacts.

## Contact Sheets

| sheetId | contactSheetPath |
| --- | --- |
| `r02-tiles-ground` | `assets-src/image/contact-sheets/dark-v1/r02-tiles-ground-contact.png` |
| `r02-tiles-wall` | `assets-src/image/contact-sheets/dark-v1/r02-tiles-wall-contact.png` |
| `r02-tiles-decal` | `assets-src/image/contact-sheets/dark-v1/r02-tiles-decal-contact.png` |
| `r03-props-interactable` | `assets-src/image/contact-sheets/dark-v1/r03-props-interactable-contact.png` |
| `r03-props-environment` | `assets-src/image/contact-sheets/dark-v1/r03-props-environment-contact.png` |
| `r03-vfx-telegraph` | `assets-src/image/contact-sheets/dark-v1/r03-vfx-telegraph-contact.png` |
| `r04-actors-player` | `assets-src/image/contact-sheets/dark-v1/r04-actors-player-contact.png` |
| `r04-actors-humanoid` | `assets-src/image/contact-sheets/dark-v1/r04-actors-humanoid-contact.png` |
| `r04-actors-monster` | `assets-src/image/contact-sheets/dark-v1/r04-actors-monster-contact.png` |
| `r04-actors-boss` | `assets-src/image/contact-sheets/dark-v1/r04-actors-boss-contact.png` |
| `r05-bestiary-humanoid-icons` | `assets-src/image/contact-sheets/dark-v1/r05-bestiary-humanoid-icons-contact.png` |
| `r05-bestiary-creature-icons` | `assets-src/image/contact-sheets/dark-v1/r05-bestiary-creature-icons-contact.png` |
| `r05-boss-icons` | `assets-src/image/contact-sheets/dark-v1/r05-boss-icons-contact.png` |
| `r06-portraits-classes` | `assets-src/image/contact-sheets/dark-v1/r06-portraits-classes-contact.png` |
| `r06-portraits-trees` | `assets-src/image/contact-sheets/dark-v1/r06-portraits-trees-contact.png` |
| `r06-portraits-zones` | `assets-src/image/contact-sheets/dark-v1/r06-portraits-zones-contact.png` |

## Raw Sheet Source Trace

| sheetId | rawSheetPath | rawSheetHash | sourceFolderLabel | sourceImageName |
| --- | --- | --- | --- | --- |
| `r02-tiles-ground` | `assets-src/image/raw/sheets/dark-v1/r02-tiles-ground.png` | `013fd5caf8fe811d22677ee5cc4a89916a603697453cee84f479266353df4135` | `codex-generated-images-dir/019e3f57-d987-7ed3-8c4c-db5de3e2631f` | `ig_0adcbb6648d3e901016a0c1eb569008191b30a35c353e9cf7e.png` |
| `r02-tiles-wall` | `assets-src/image/raw/sheets/dark-v1/r02-tiles-wall.png` | `1656e148a7f578c16d1f79608da4278ec59302fca8642b8bbe1c931c61708513` | `codex-generated-images-dir/019e3f5a-2ef2-7b22-ab4f-76f9d2593516` | `ig_0d90186b44acbc86016a0c1f5060f48191b16358ea63bbe9eb.png` |
| `r02-tiles-decal` | `assets-src/image/raw/sheets/dark-v1/r02-tiles-decal.png` | `ec82dacc6b797261e27e42d17174080202a1fbec0a614c4516d9f721a69a5e1f` | `codex-generated-images-dir/019e3f59-131c-7f73-8704-09d58cfddf72` | `ig_0cd734868160e8a3016a0c1f0786d48191862cbdfe8c7cfc73.png` |
| `r03-props-interactable` | `assets-src/image/raw/sheets/dark-v1/r03-props-interactable.png` | `b55431620839c5947139fea7b2a28df2964e7af917b1e052ff4f7db63462b720` | `codex-generated-images-dir/019e3f5c-9bfd-73e3-8f51-79d6c2b92e4d` | `ig_040de379e1efef3e016a0c1fed93d881918583230fa1ec7c7c.png` |
| `r03-props-environment` | `assets-src/image/raw/sheets/dark-v1/r03-props-environment.png` | `f856aa6ff9838140020fd487a05542186d3fc8bd4982fe0492237a1ad0922ec5` | `codex-generated-images-dir/019e3f5b-5d2f-7e31-a3b3-8f309b8650d5` | `ig_0a95392a475d0743016a0c1f9cde648191bce3d907fa420689.png` |
| `r03-vfx-telegraph` | `assets-src/image/raw/sheets/dark-v1/r03-vfx-telegraph.png` | `13de8618e65da84480160757b53d8d93c1591d66687d0a8452dd85885c1facdf` | `codex-generated-images-dir/019e3f5e-0578-7c10-a7c8-dddae43ff695` | `ig_08811cac063e6b7d016a0c204ab7408191a95cbbef3eb701ae.png` |
| `r04-actors-player` | `assets-src/image/raw/sheets/dark-v1/r04-actors-player.png` | `bd6be9f3407443fa4d2801bb348083490ca8923f2caba35f940619bf7c16700c` | `codex-generated-images-dir/019e3f65-c3ab-7a01-b593-c543ceafe84a` | `ig_0f4fa6fd07a55feb016a0c224654948191b5ba252f4795ddc3.png` |
| `r04-actors-humanoid` | `assets-src/image/raw/sheets/dark-v1/r04-actors-humanoid.png` | `f8d2a1a436244e2f393cf625ec292c7e9730070d876e3100b8d646d765e7cd73` | `codex-generated-images-dir/019e3f60-b8b0-7cf0-a929-8d409036d784` | `ig_0babbf1433a34f43016a0c20fa61288191a0236af702be810e.png` |
| `r04-actors-monster` | `assets-src/image/raw/sheets/dark-v1/r04-actors-monster.png` | `fe94015eb6a2ba31077cac62d604acdeaee336f5c72878d4f86c9070b7cb6bdb` | `codex-generated-images-dir/019e3f63-f9fd-7560-97ff-7ac058af36de` | `ig_0e318a9783b712b5016a0c21d22ff4819186f81c5cd130e850.png` |
| `r04-actors-boss` | `assets-src/image/raw/sheets/dark-v1/r04-actors-boss.png` | `2419e3e6440eea366c09167f01741fe473d1decc999519404a61527a69e3ee80` | `codex-generated-images-dir/019e3f5f-580a-7d91-bb80-04b9de909868` | `ig_0a4791196ec42dbc016a0c20a0eefc8191b3e8106288a2cde6.png` |
| `r05-bestiary-humanoid-icons` | `assets-src/image/raw/sheets/dark-v1/r05-bestiary-humanoid-icons.png` | `e87e76ec4d0b93e552043b60617d3d78dd348f62d585c68a23cfc1bc1f48e0a0` | `codex-generated-images-dir/019e3f68-f400-7663-9221-f6fda26f5841` | `ig_01dde8382bcf115d016a0c2317b34c8191bd7aa89a6315b6bf.png` |
| `r05-bestiary-creature-icons` | `assets-src/image/raw/sheets/dark-v1/r05-bestiary-creature-icons.png` | `9405e9cf9e6c718e53afbdd203c209c43d35e43d400d3769d477369b86f66a2d` | `codex-generated-images-dir/019e3f67-18d6-7231-bb64-7c5afb6ba82b` | `ig_091b00fdacd94c36016a0c229ecd088191acdb7ad713267925.png` |
| `r05-boss-icons` | `assets-src/image/raw/sheets/dark-v1/r05-boss-icons.png` | `c37d0d47d7881b8e7f63fec6b1160d1fed9ac612e0dac9dfaf180f9ecd9838ca` | `codex-generated-images-dir/019e3f6a-9d58-7523-966f-7a33a68d2d40` | `ig_0e55b5a59130b0a9016a0c23874fc0819188ef5ed32f3a9faf.png` |
| `r06-portraits-classes` | `assets-src/image/raw/sheets/dark-v1/r06-portraits-classes.png` | `b29a85d2770b45cc6eb0be820cbd95825223797711a0e82b57f0c0065f1804a3` | `codex-generated-images-dir/019e3f6b-c64a-7672-90d2-3038ba56a149` | `ig_0bb69eac41ea104b016a0c23ceccfc819181a48d1d3ba13e21.png` |
| `r06-portraits-trees` | `assets-src/image/raw/sheets/dark-v1/r06-portraits-trees.png` | `d016cd8bfc5116f006a0cac4fc74a603b6a7625513673c845ab8248b45ab7d76` | `codex-generated-images-dir/019e3f6d-1f2e-7dd3-a20a-303775422629` | `ig_04472cf129b226cb016a0c242890388191b8ea76759b6c6529.png` |
| `r06-portraits-zones` | `assets-src/image/raw/sheets/dark-v1/r06-portraits-zones.png` | `7929a0c9d1d1c6a5d1b9472672bb73125124ae7eed1fd0f5b515f5b69e87cf0c` | `codex-generated-images-dir/019e3f70-dc8f-7a41-b606-391320836675` | `ig_08848711681738e7016a0c251c00f88191ab10c63ac04c8bac.png` |

## Contact Sheet Art QA

Checked against:

1. `UI/pr/dark-uiux-pr05-map-actor-portrait-replacement.md`
2. `UI/review/open-design/ktome-dark-ui-design.md`
3. `UI/review/open-design/dark-uiux-pr05-map-actor-portrait-design.md`

| Surface | Result | Evidence |
| --- | --- | --- |
| fixed grid and one subject per cell | `PASS` | `darkSpriteSheetLint` and `spriteSheetMapLint` passed for all 16 PR05 sheet ids; contact sheets show no accepted cross-cell or merged subjects after repack |
| no baked text, labels, logos, or watermarks in art cells | `PASS` | contact QA found no text baked into the assets; contact sheet row/col labels are generated review chrome only |
| dark fantasy style fit | `PASS` | sheets use low-saturation charcoal, worn stone, iron, leather, ember, and restrained cyan accents; no sci-fi, chibi, glossy plastic, or office vector drift accepted |
| tile, decal, and prop readability | `PASS` | ground and wall tiles keep walkability readable; decals remain environmental overlays; props read as interactable or environmental set pieces |
| actor silhouette at gameplay scale | `PASS` | `build/reports/verification/dark-uiux/contact-qa/small-actors.png` keeps actor categories readable at 32px; player, humanoid, monster, and boss silhouettes remain separable from tiles |
| icon and VFX readability at gameplay scale | `PASS` | `build/reports/verification/dark-uiux/contact-qa/small-icons-vfx.png` keeps monster family, boss icon, terrain interaction, and telegraph categories readable at 32px |
| category perspective | `PASS_WITH_NOTED_RISK` | actors are full-body gameplay tokens and portraits are framed bust/scene portraits. Some player and boss sprites are rich three-quarter figures, but they are still accepted as map actors because the 32px and golden checks preserve gameplay readability |
| telegraph layer safety | `PASS` | telegraph assets are rings/arcs/open sigils rather than dense blocks; PR05 and phase4 UI/UX golden hashes were rebaselined after visual inspection |
| transparent-background preview artifacts | `PASS` | `view_image` shows checkerboard for transparent pixels on some portrait previews; black-background compositing in `build/reports/verification/dark-uiux/contact-qa/black-preview/` did not show hard white UI pollution |

## Golden Evidence

| label | scenarioId | seed | artifact | hash |
| --- | --- | ---: | --- | --- |
| `dark-uiux-pr05-map-layer-stack` | `dark-uiux-pr05-map-layer-stack` | `202605090501` | `client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-map-layer-stack.png` | `69aa65eea07e8e107fe16caa16502fd2873c283db5c8bd02bfaf47fa37592ec4` |
| `dark-uiux-pr05-actor-boss-telegraph` | `dark-uiux-pr05-actor-boss-telegraph` | `202605090502` | `client/build/reports/golden/dark-uiux-pr05/dark-uiux-pr05-actor-boss-telegraph.png` | `0411dce58d13f15bb9cdfc7e83e76a7834433462c5452db89f18b7f77d90261a` |

## Manual Whitebox Status

| label | runtime scenario registration | result | notes |
| --- | --- | --- | --- |
| `dark-uiux-pr05-map-layer-stack` | `ValidationScenarioRegistry`, `ValidationScenarioPresentationCatalog`, `i18n`, `ClientSmokeHarnessTest`, `GoldenScreenshotHarnessTest` | `SKIPPED_BY_REQUEST` | Automated scene, smoke, and golden coverage are present; packaged CUA is not claimed in this run. |
| `dark-uiux-pr05-actor-boss-telegraph` | `ValidationScenarioRegistry`, `ValidationScenarioPresentationCatalog`, `i18n`, `ClientSmokeHarnessTest`, `GoldenScreenshotHarnessTest` | `SKIPPED_BY_REQUEST` | Automated scene action, smoke, and golden coverage are present; packaged CUA is not claimed in this run. |

## Validation Run

All Gradle commands were run serially after `source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env`.

| Gate | Result | Notes |
| --- | --- | --- |
| `./gradlew syncPhase2Manifests manifestLint assetLint styleLint darkKeyRegistryLint darkSpriteSheetLint spriteSheetMapLint -Pktome.darkUiux.spriteMapReport=assets-src/image/manifests/dark-v1-pr05-sprite-map-report.jsonl -Pktome.darkUiux.spriteMapReportSheetIds=...` | `PASS` | runtime manifest synced from canonical visual/audio manifests; PR05 sprite map report limited to the 16 owner sheets |
| `./gradlew darkManifestCoverageLint -Pktome.darkUiux.coverageMode=owner-scope -Pktome.darkUiux.ownerPr=PR-05 -Pktome.darkUiux.requiredOwnerSheetIds=...` | `PASS` | coverage report has `ownerMissingKeys=[]`, `ownerPendingKeys=[]`, `ownerOldStyleKeys=[]`, `allowedOwnerFallbackKeys=[]`, and all 16 owner sheet ids |
| `./gradlew :client:test --tests com.ktome.client.assets.ManifestResolveTest --tests com.ktome.client.render.TileLayerComposerTest --tests com.ktome.client.render.TileRendererCanvasTest` | `PASS` | resolver, layer ordering, canvas/readability focused tests |
| `./gradlew :client:goldenScreenshot` | `PASS` | first run failed only because the accepted Codex resource replacement changed screenshot hashes; expected hashes were rebaselined, then the gate passed |
| `./gradlew :client:clientSmoke maintainabilityLint verifyChanged` | `PASS` | verifyChanged routed `client-ui-evidence`, `dark-uiux-pipeline`, `dark-uiux.manifest`, and `resource-pipeline` domains |
| `git diff --check` | `PASS` | no whitespace errors in tracked diff |
| absolute path scan over modified and untracked committed-candidate files | `PASS` | no machine absolute paths found |

## Self-Audit

| Contract | Result |
| --- | --- |
| raw sheets generated by internal Codex CLI wrapper | `PASS`; each accepted raw sheet has a `codex-generated-images-dir/<session-id>` source summary and `sourceImageName` |
| exact owner inventory exists and matches PR05 owner scope | `PASS`; `UI/sprite-sheets/pr05-owner-key-inventory.json` / `.md` materialize 16 sheets and 161 keys |
| PR-02-2 upstream keys not reclaimed | `PASS`; upstream owner keys remain outside PR05 ownership |
| PR05 owner keys resolve to `dark-v1/` and no `missing_visual` survives | `PASS`; owner-scope coverage reports no missing, pending, old-style, or fallback owner keys |
| sheet plan, key registry, canonical manifest, runtime manifest, raw/contact/runtime PNGs are in one pipeline | `PASS`; resource lint and sprite map lint passed |
| map actor/prop/VFX layer contracts are covered by tests | `PASS`; composer/canvas focused tests and PR05 golden labels cover the contract |
| art style matches PR05 and Open Design requirements | `PASS_WITH_NOTED_RISK`; all hard QA criteria pass. The noted risk is that several large actor and portrait assets are visually rich, but they remain category-correct and readable at the target display sizes |
| gameplay rule or content authority changed | `PASS`; this PR only changes visual resources, manifest wiring, tests, pipeline scripts, and evidence |
| committed evidence paths are repo-relative | `PASS`; absolute path scan found no machine absolute paths in changed or untracked committed-candidate files |
| whitebox validation | `SKIPPED_BY_REQUEST`; this record does not claim manual packaged CUA pass. Automated scenario registration, client smoke, and golden evidence cover the PR05 labels for this run |

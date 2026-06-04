# PR08 Non-Ruins Family-Ghost Material Authority Probe

Date: 2026-06-03

## Decision

Rejected. Do not promote the temporary `0.38` family-ghost alpha.

This probe tested whether stronger clipped family plate authority could recover
the packaged forest-edge and shadow-depths topology-risk crops after
`family-ghost V1`. It did not. The board shows that `0.38` mostly raises the
same topology-source/grid read instead of restoring previous V4 environment
structure.

Review board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-family-ghost-material-authority-2026-06-03/alpha038/non-ruins-family-ghost-alpha038-ab-board.png`

Hash: `3d39330d889bcbf6dd04f4596bce8b482fd99181721582758cbe46fd5e58a32e`.

## Evidence

Detailed hashes are recorded in:

`UI/review/dark-uiux-pr08-exploration/non-ruins-family-ghost-material-authority-2026-06-03/evidence-index.tsv`

Temporary alpha `0.38` packaged map hashes:

1. Forest-edge:
   `ee55a042d3653c803d235a51683e6fc9f0c1448c71a4af19e0040f753f05c6ab`
2. Shadow-depths:
   `1ab762b5dbdc045da78586f06d275aac310c28066e219f7a4ed65a4cacbb1bc3`

## Validation

Commands actually run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-forest-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 16s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 1s`.

Packaged app captures were run for forest-edge and shadow-depths with
`scripts/capture-macos-app-window.sh`, followed by fixed crop derivation from
the full-window captures.

The runtime constant was restored to the accepted-forward `0.22` value after
visual rejection. This probe is evidence only and must not be used as a runtime
closure packet.

## Next Action

Stop increasing family-ghost opacity as a standalone tactic. The next
meaningful packet should change topology-risk map-stage structure, not merely
make the current hybrid layers brighter.

# PR08 Non-Ruins Family-Ghost Layer-Order Probe

Date: 2026-06-03

## Decision

Rejected. Do not promote the temporary `source-then-family-ghost` layer order.

The probe moved the low-alpha family plate ghost after the dedicated topology
source band to test whether the family material could restore authored-room
quality without increasing alpha. It failed the visual bar. Forest-edge is
nearly unchanged, and shadow-depths still reads as a large tactical/debug floor
with auxiliary rails. This confirms that the problem is not only opacity or
draw order; the chopped topology-band approach is the wrong visual direction
for director-grade packaged parity.

Review board:

`UI/review/dark-uiux-pr08-exploration/non-ruins-family-ghost-layer-order-2026-06-03/source-then-family-ghost/non-ruins-family-ghost-layer-order-ab-board.png`

Hash: `fd7b7f74417b7c32cec815da6f7668930f95bae025b9e78ed5d404e9a5d6db9d`.

## Evidence

Detailed hashes are recorded in:

`UI/review/dark-uiux-pr08-exploration/non-ruins-family-ghost-layer-order-2026-06-03/evidence-index.tsv`

Temporary layer-order packaged map hashes:

1. Forest-edge:
   `229c46172d4476322fd7202857284fc7d6153b036357664ddc1a0913e996f0e8`
2. Shadow-depths:
   `d57eb9a3c473adab0e032cce35e8bed9d724c5d3fb99fca119be48d6e132930c`

## Validation

Commands actually run:

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew :client:packageMacApp preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-forest-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 12s`.

```bash
source "$HOME/.sdkman/bin/sdkman-init.sh" && sdk env && ./gradlew preparePhase4V4Whitebox -Pktome.whitebox.scenario=dark-uiux-pr08-director-shadow-depths-map-stage --no-configuration-cache
```

Result: passed. `BUILD SUCCESSFUL in 1s`.

Packaged app captures were run for forest-edge and shadow-depths with
`scripts/capture-macos-app-window.sh`, followed by fixed crop derivation from
the full-window captures.

The runtime draw order was restored after visual rejection. This probe is
evidence only and must not be used as a runtime closure packet.

## Next Action

Stop ghost-band draw-order tuning. Any next attempt must change the larger
map-stage presentation structure and should be stopped immediately if the
first visual board loses UI/UX quality.

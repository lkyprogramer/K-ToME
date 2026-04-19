# macOS Packaging Assets

This directory owns packaging-only assets for `:client:packageMacApp`.

Current source files:

- `K-ToME-app-icon.png`
  - Packaging-only macOS app icon source.
  - Generated through the repo-owned Gemini asset pipeline from `assets-src/image/specs/macos-app-icon-plan.yaml`.
  - This asset does not enter the runtime visual manifest or game content contract.

Build outputs:

- `client/build/jpackage/icon/K-ToME.icns`
- `client/build/release/K-ToME.app`

Current scope:

- Local macOS `.app` image only
- No `dmg`
- No codesign
- No notarization

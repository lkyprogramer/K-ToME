# K-ToME macOS Packaging

## Scope

This note records the repo-owned packaging icon and app-image path for the local macOS build.

## Asset Flow

1. Gemini plan:
   - `assets-src/image/specs/macos-app-icon-plan.yaml`
2. Raw generation output:
   - `assets-src/image/raw/generated/packaging/macos/K-ToME-app-icon.png`
3. Processed output:
   - `assets-src/image/processed/packaging/macos/K-ToME-app-icon.png`
4. Packaging source synced into repo:
   - `client/src/packaging/macos/K-ToME-app-icon.png`
5. `.icns` output:
   - `client/build/jpackage/icon/K-ToME.icns`
6. macOS app image:
   - `client/build/release/K-ToME.app`

## Commands

```bash
./scripts/generate_macos_app_icon.sh
./gradlew :client:prepareMacAppIcon
./gradlew :client:packageMacApp
```

## Current Boundary

1. The app icon is a packaging-only asset.
2. It does not enter the runtime visual manifest.
3. `packageMacApp` consumes the committed PNG and never calls Gemini at build time.
4. Signing, notarization, and `.dmg` packaging remain out of scope.

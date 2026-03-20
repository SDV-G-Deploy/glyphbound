# Glyphbound (v0.1.2)

Android ASCII-like roguelite prototype with deterministic procedural generation.

## V1-3 highlights
- `GlyphMapView` custom renderer extracted from `MainActivity`:
  - monospace paint (`Typeface.MONOSPACE`)
  - stable per-cell width/height from font metrics
  - DPI-safe canvas drawing
  - high-contrast palette support
  - no heavy animations
- Difficulty profiles: `EASY`, `NORMAL`, `HARD`
  - affect generation (`wallChance`, `riskChance`)
  - affect validator strictness (`EDGE` vs `NODE`, min disjoint paths)
  - affect gameplay (`startingHp`, risk damage multiplier)
- Reproducibility key: `seed + profile`
- New test coverage:
  - lightweight golden snapshot for glyph buffer
  - palette high-contrast policy test
  - validator policy tests for EDGE/NODE behavior
  - deterministic retry reproducibility for fixed `seed+profile`

## Modules
- `app` — Android UI/input/render loop
- `core:model` — domain model + difficulty profile + glyph render buffer
- `core:rules` — movement and damage rules
- `core:procgen` — deterministic generation + path validation API

## Run
```bash
./gradlew :app:assembleDebug
```
APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Choosing difficulty profile
By default: `NORMAL`.

You can pass extras at launch:
- `seed` (Long)
- `profile` (`EASY|NORMAL|HARD`)

Or switch in-app via the profile button in HUD.

## Reproduce same map
Use the same pair:
- `seed`
- `profile`

Generator key is derived as deterministic `seedWithProfile(seed, profile)`.

## Tests
```bash
./gradlew test
```
Includes:
- render golden snapshot (`GlyphRenderSnapshotTest`)
- high-contrast palette expectation test
- connectivity and disjoint validator policy tests
- NODE stricter-than-EDGE controlled case
- deterministic retry reproducibility for fixed `seed+profile`

## CI
`.github/workflows/android-debug.yml` runs:
1. `./gradlew test`
2. `./gradlew :app:assembleDebug`
3. Uploads debug APK artifact

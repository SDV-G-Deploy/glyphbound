# Glyphbound (v0.1.1)

ASCII-like Android prototype with deterministic procedural generation.

## Modules
- `app` — Android UI/input/render loop
- `core:model` — domain model (`Tile`, `Level`, `GameState`)
- `core:rules` — movement and damage rules
- `core:procgen` — deterministic generation + path validation API

## V1-2 changes
- Readability/mobile UI pass:
  - monospace render with stable line spacing
  - HUD with explicit `HP`, `Seed`, `Steps`
  - short legend of glyph meanings
  - bigger D-pad touch targets
  - optional swipe input on map area
- New high-contrast palette toggle in-app (`High contrast` switch).
- Procgen validator upgraded from "2 shortest paths" to disjoint-path validation:
  - `PathValidator.validate(...)` in `core:procgen`
  - supports `DisjointMode.EDGE` and `DisjointMode.NODE`
  - generator retries deterministically (`seed + attempt`) until valid map

## Run
```bash
./gradlew :app:assembleDebug
```
APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Tests
```bash
./gradlew test
```
Included tests:
- reproducibility: same seed => same map
- connectivity: `S` always reaches `E`
- disjoint-path validator: fail and pass fixtures
- risk rule: stepping on `~` reduces HP

## CI
`.github/workflows/android-debug.yml`:
1. runs `./gradlew test`
2. builds debug APK
3. uploads APK artifact

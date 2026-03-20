# Glyphbound (v0.1.0)

ASCII-like Android prototype with deterministic procedural generation.

## Vision
Glyphbound is a compact, replayable mobile dungeon loop where each run is bound to a seed.
Current goal: stable architecture + deterministic level generation + minimal playable core.

## Modules
- `app` — Android UI + input loop + render
- `core:model` — domain model (`Tile`, `Level`, `GameState`)
- `core:rules` — movement, risk handling, path validators
- `core:procgen` — deterministic seed pipeline + map generation/fallback

## Run
```bash
./gradlew :app:assembleDebug
```
APK path:
`app/build/outputs/apk/debug/app-debug.apk`

## Seed reproducibility
Default seed is `1337`.
To launch with custom seed (adb):
```bash
adb shell am start -n com.sdvgdeploy.glyphbound/.MainActivity --el seed 424242
```
Same seed -> same generated map (for same build/config).

## Procgen validation rules
- Connectedness: start (`S`) must reach exit (`E`)
- At least 2 shortest routes from `S` to `E`
- If validation fails repeatedly, deterministic fallback map is generated

## V1 / V2 / V3 roadmap (short)
- **V1**: deterministic maps, movement, risk tile, release pipeline
- **V2**: entities (mobs/items), fog-of-war, combat tick, save/load
- **V3**: progression/meta-layer, biome set, live-ops balancing seeds

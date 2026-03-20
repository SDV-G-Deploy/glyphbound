# Glyphbound (v0.2.2)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-3 highlights
- **Staged effect pipeline** in `core:rules`:
  - `reduce(state, intent)` (pure + deterministic intent reduction)
  - `applyReactions(...)` (materialize reaction effects into hazard zones/tile transitions)
  - `tickHazards(...)` (TTL decay + tile restoration)
  - `resolveDamage(...)` (HP/final flags)
  - `buildCombatLog(...)` (player-facing log synthesis)
  - `step(...)` is now a thin orchestrator for these stages.
- **Hazard visualization in map + HUD**:
  - map overlays: `^` (fire zone), `!` (shock zone)
  - HUD legend aggregates active hazard types and max remaining TTL
  - high-contrast palette includes dedicated overlay colors.
- **Type-specific spread profiles (bounded chains)**:
  - fire spread profile + shock spread profile in `DifficultyProfile.EnvTuning`
  - per-profile knobs: `spreadChance`, `maxTargets`, `maxChainDepth`
  - bounded BFS spread + deterministic rolls (seed/move/position/depth/type)
- **Invariants/test expansion**:
  - stage-by-stage pipeline composition tests
  - long sequence invariants on fixed seed/path (no runaway chain, bounded TTL/HP)
  - visual mapper and overlay rendering tests.

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, hazard/tile definitions, glyph rendering
- `core:rules` — reducer + staged effect pipeline + hazard/environment rules
- `core:procgen` — deterministic generation + path validation API

## Pipeline schema
```text
Intent (Move)
  -> reduce(state, intent)
  -> applyReactions(reduced)
  -> tickHazards(reactionState)
  -> resolveDamage(ticked)
  -> buildCombatLog(resolved)
```

## Hazard visualization model
- `HazardVisualizationMapper.fromState(GameState)` builds:
  - overlay map (`Pos -> glyph`) for tile-level hazard indication
  - legend entries grouped by hazard type (`count`, `maxTtl`)
- UI renders overlays through `GlyphRender.buildBuffer(..., hazardOverlays)` and shows compact legend in HUD.

## Spread profile tuning
Configured in `DifficultyProfile.EnvTuning`:
- `fireSpreadProfile = SpreadProfile(spreadChance, maxTargets, maxChainDepth)`
- `shockSpreadProfile = SpreadProfile(spreadChance, maxTargets, maxChainDepth)`

These constraints guarantee bounded behavior and prevent runaway chain reactions.

## Tests
```bash
./gradlew test
```

Coverage includes:
- reducer transition and deterministic sequence checks
- staged pipeline integration tests
- hazard TTL decay and tile transition checks
- bounded chain reaction checks
- long sequence invariants
- hazard visualization mapper and overlay rendering.

## Release lane (existing flow, unchanged)
Workflow: `.github/workflows/android-release.yml`

- Tag push `v*` runs tests, builds release APK and uploads release asset.
- If signing secrets exist → signed APK; otherwise unsigned fallback APK.
- CI/infra/workflows were intentionally not modified in this iteration.

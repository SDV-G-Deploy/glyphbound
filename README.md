# Glyphbound (v0.2.1)

Android ASCII-like roguelite prototype with deterministic procedural generation and a ViewModel-driven state store.

## V2-2 highlights
- **Reducer / Effect architecture** in `core:rules`:
  - `reduce(state, intent)` is pure and deterministic
  - `processEffects(result)` applies tile mutations, hazard TTL decay and log synthesis
  - `step(...)` is a thin orchestrator (`reduce -> processEffects`)
- **Expanded env-system**:
  - persistent hazard zones with predictable TTL decay
  - tile transitions:
    - `oil -> fire -> ash`
    - `water + spark -> shocked water -> water`
  - bounded chain reaction (radius-1 neighborhood, profile-limited target count/chance)
  - damage logs explain source (`hazard tile`, `persistent hazard`, reaction events)
- **Difficulty-integrated tuning table** (`DifficultyProfile.EnvTuning`):
  - hazard damage multiplier
  - fire/shock persistent TTL
  - chain reaction chance and max targets
- **Signed release lane with graceful fallback**:
  - if Android signing secrets are available → signed APK
  - if secrets are absent → unsigned APK with explicit CI log status
  - release asset name clearly indicates `signed` / `unsigned`

## Modules
- `app` — Android UI/input/render loop + `GameViewModel`
- `core:model` — domain model, immutable state, hazard/tile definitions, glyph rendering
- `core:rules` — reducer + effect processor + hazard/environment rules
- `core:procgen` — deterministic generation + path validation API

## Reducer / Effect schema
```text
Intent (Move)
  -> reduce(state, intent)
      => ReduceResult(newState, effects, events)
  -> processEffects(result)
      => applies tile transitions + hazard TTL ticks + damage/log aggregation
```

## Tests
```bash
./gradlew test
```

Added/updated test coverage for:
- reducer transition (move)
- hazard TTL decay
- tile transitions
- bounded chain reaction behavior
- determinism of fixed seed+profile+intent sequence

## Release lane (signed + fallback)
Workflow: `.github/workflows/android-release.yml`

Secrets (optional):
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Behavior:
- Secrets present: Gradle builds signed `app-release.apk`, uploaded as `glyphbound-<tag>-signed.apk`
- Secrets missing: CI falls back to `app-release-unsigned.apk`, uploaded as `glyphbound-<tag>-unsigned.apk`

No signing material is stored in repository.

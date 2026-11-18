# Plan: celestials (Enhanced Celestials 2 compatibility)

Branch `feature/celestials`, from `origin/main` (UI-first, PR #36, merged). Source of the rules: `docs/superpowers/specs/2026-11-17-enhanced-celestials-compat.md`; the owner approved all eleven recommendations of its section (f). Checklist and facts read from the jars: `docs/parts/celestials.md`.

## Model

- An EC event is a **second layer** (`LunarMood`), never a replacement of the real moon. Without EC (or with `parts.celestials` off) the source is empty and every factor is 1.
- New part `celestials` (switch `parts.celestials`, default on, **no required mod**: EC is looked up at runtime, so the part's tests and the part-off matrix run work in the EC-less test runtime). Reflection only (EC is All Rights Reserved), 0 mixins, pure logic in `core/moon`.
- Differences from the spec, found while checking the jars: (1) the part has no required mod (the spec said `enhancedcelestials2core`, which would make the part always off in CI); (2) EC does not tint the fog or the sky dome, so `EclipseClient` skips only its **moon disc** when EC colours the moon; (3) our eclipse event's sky light is `c0403a`, not `8c0808`, because EC multiplies the lightmap by it (to be judged in game); (4) our eclipse mirror is removed at our own dawn, because an EC event instance would run to the end of the day number (about 780 ticks longer).

## Tasks

1. **Pure rules** (`core/moon`): `LunarMood` (kinds, super flag, `ofEvent`, the factors, `scaled`), `LunarSource` (+ `Upcoming`), `LunarMoods` (facade, `Tuning`, fail-soft), `EclipseSignal`. JUnit (`LunarMoodTest`, `LunarMoodsTest`, `EclipseSignalTest`).
2. **The part**: `Part.CELESTIALS`, `CelestialsPart`, `CelestialsConfig` (six keys), Admin rows and section, `AdminCoverageTest` entry, mods.toml optional dependency, `LinkedMods.TESTED`, lang. `PartLoaderTest` follows the new part.
3. **The bridge**: `EcAdapter`, `EcReflect` (resolved once; no change listener), `EcSource`. `EcReflectTest` runs it against stand-in classes with EC's names (`src/test/java/dev/corgitaco/...`, javap signatures).
4. **Mapping**: Blue Moon and super moon rite cost (a factor in `RiteCost.begin`, no new mixin), Blood Moon bite (`BloodMoonBite`, `BloodDrinkEvent` at LOWEST), Super Blue Moon (`ForcedShift.addForcedSpare`), the harvest tag (`data/enhancedcelestials2defaultlunarevents/tags/item/harvest_moon_crops.json`). (The NPC pack chance and the boss weight hooks and keys were dropped in review: plans 15 and 17 add them back.)
5. **The eclipse mirror**: `EclipseSignal` fired by `LunarEclipse.changed` (our chat line is always sent: EC announcing a mirrored event is unverified), the datapack event and its three modifiers (no `text_components`: our chat line is the only notice), `EclipseClient` skipping its moon disc.
6. **Hub**: `HubAlmanac.next/listed/step/ringColor`, `LunarMoodClient` (a read from EC once a second), `AlmanacEvents` (ring, line, name with a titled fallback), the three Path pages. Dev shots: `-Dsupernaturalbloodlines.devshots.lunar=true` installs stand-in events (`DevDemo.lunar`).
7. **GameTests** with `FakeEc`: nothing changes without EC, every mood, the schedule, the mirror and our chat line, a broken source, bite, forced shift, rite cost; `CelestialsPartOffGameTests` for the switch.
8. **Build and matrix**: `-PecJars=<folder>` adds EC's four jars (downloaded by hand) to `localRuntime`, off by default. CI (`.github/workflows/build.yml`) gets the steps `-PpartsOff=celestials` and `"-PpartsOff=species,artifacts"`; the headless real-EC checks are in `docs/parts/celestials.md`.
9. **Docs, wiki, changelog**; shots with `runSafeShots` under `focus-watch.ps1` (never a visible client).

## Rules for the work

TDD where the logic is pure; GameTests use the fake, never EC; no mixin; every config key has an Admin row; `-PpartsOff=celestials` must leave the game as it is without EC; no push to main, the PR waits for green CI and the review.

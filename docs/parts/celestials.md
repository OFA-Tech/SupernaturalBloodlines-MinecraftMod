# Part: celestials

Requires: **no mod**. Enhanced Celestials 2 (EC) is optional and found at runtime (`ModList.isLoaded("enhancedcelestials2core")`); without it the part installs an empty lunar source and every factor is 1, so the game plays as before. Package: `net.ofatech.supernaturalbloodlines.parts.celestials`. Config key `celestials`, file `supernaturalbloodlines-celestials.toml`. Plan: `docs/superpowers/plans/2026-11-18-celestials.md`. Spec: `docs/superpowers/specs/2026-11-17-enhanced-celestials-compat.md`.

**Mixin budget: 0 of 3** (see `docs/mixins.md`).

## The rule

An EC event never replaces the real moon. `MoonRule.isFullMoon*`, the rites' "needs the full moon" check, the Werewolves forced shift and the Werewolves `Helper` keep reading the real phase and time. An EC event is a second layer, the **lunar mood** (`core/moon/LunarMood`: `NONE`, `BLOOD`, `BLUE`, `HARVEST`, `ECLIPSE`, plus a `superMoon` flag), read through `LunarMoods` (`core/moon`). A source that throws gives "no mood", never an exception on a tick.

## What each event does (all factors are config, 1 = off)

| EC event | Effect | Key |
|---|---|---|
| Blood Moon (any phase) | a vampire's bite gives x1.25 blood (rounded half up, an emptied bite stays empty). | `blood_moon_bite_bonus` |
| Blue Moon (any phase) | every rite costs x0.8 altar power (the species part's `RiteCost.begin`). It never lets a full-moon rite run on another night | `blue_moon_rite_cost` |
| Harvest Moon | our crops (vervain, the two wolfsbane strains) are in EC's item tag `enhancedcelestials2defaultlunarevents:harvest_moon_crops` (a datapack file, `required: false`), so they drop x2 | none (unverified: no doubled drop has been seen in a game) |
| any super moon (full moon only) | Rite of the Moon, Silver Moon and Immortality cost x0.75, on top of the Blue Moon factor. | `super_moon_rite_cost` |
| Super Blue Moon | the Werewolves forced shift is skipped for everybody (`ForcedShift.addForcedSpare`); the shift at will still follows `moon_bound_werewolves` | `super_blue_skips_forced_shift` |
| our lunar eclipse | see below | existing `lunar_eclipse_chance` |

## The eclipse mirror

Our roll stays the source of truth (`LunarEclipse`, 3%, its SavedData, its gameplay). When it starts, `EclipseSignal` tells the part, which asks EC to start our event `supernaturalbloodlines:lunar_eclipse` tonight (`setLunarEventTonight`); when it ends, EC's event is removed (`removeLunarEvent(0)`). The event is a datapack file of ours (`data/supernaturalbloodlines/enhancedcelestials2core/lunar/event/lunar_eclipse.json` and three modifiers: sky light, moon colour, name colour). EC then draws the moon colour and light, and the hub's almanac can name it. **Our own chat line is the only notice**, mirrored or not: the event has **no `text_components` modifier**, so EC sends none. **Owner rule 9 (EC's notice replaces ours) is deferred**: it needs a real-game check that EC announces a mirrored start and a `removeLunarEvent` end, and then the modifier can be added back and our line skipped (see the checklist). If EC already runs another event the mirror is skipped and the gameplay still runs.

Client: `EclipseClient` keeps its fog tint and its red sky disc always, and skips only its moon disc when this client's own EC forecast says the running event is ours (`LunarMoods.ecDrawsEclipse`). A client without EC on a server with EC still gets all of ours.

## The hub

The almanac of the three Path pages (Grimoire `PathPage`, Blood Moon `BloodMoonPathPage`, Tensura `StagePathPage`) draws a ring in the event's name colour on the day's moon (tooltip: "Blood Moon in 3 days.") and a line "Blood Moon in 3 days." under the full-moon line. On the Grimoire the almanac is pinned at the foot of the left page when there are events (`Frame.reserve`), so the scrolling ladder never hides it; without events it stays in the flow as before. Every event of the six-day row is a text line, in the theme's ink (the event colour is only on the ring), wrapped so the day count is never cut; the ring has a tooltip. The icons are spaced so neighbouring rings never touch (`HubAlmanac.step`). The data is `LunarMoodClient`, read from EC once a second and cached. Without events the pages are exactly as before.

## Layout

- `core/moon/` (pure but `Level` in the source interface): `LunarMood`, `LunarSource`, `LunarMoods` (the facade and the `Tuning` numbers), `EclipseSignal`.
- `parts/celestials/`: `CelestialsPart` (init, install), `CelestialsConfig`, `EcAdapter` (what the part needs), `EcReflect` (the only class that names EC types, by reflection; no change listener: nothing needs it, and one would run inside EC's tick), `EcSource`, `BloodMoonBite` (links Vampirism), `SuperBlueMoon` (links Werewolves), `gametest/` (`FakeEc` and the tests).
- Hooks in other parts: `RiteCost` (species), `ForcedShift.addForcedSpare` (shared), `LunarEclipse.changed` and `EclipseClient` (the notice and the moon disc), `HubAlmanac`/`AlmanacEvents` (hub).

## Checked against the EC jars (core 2.0.3.4, Default Lunar Events 2.0.1.4, by javap)

- The change listener (`registerLunarEventChanged`, `onLunarEventChanged(ServerLevel, Holder previous, Holder new)`) was read too, and is **not used**: it was only a debug log, and a throwing listener would run inside EC's tick.
- An event instance runs from `day*24000 + night_start_time (12500)` (or "now" for `setLunarEventTonight`) to `day*24000 + 23999`, the end of the day number. Our eclipse ends at 23216, so the mirror is removed at our end (`removeLunarEvent(0)` shortens the active instance to "now").
- `setLunarEventTonight` has no clear-sky check (only the command and the scheduler have); it throws on the client.
- EC's client does **not** tint the fog or the sky dome: its mixins change only the moon colour, texture and size and the lightmap's sky colour. So our fog and sky tint stay ours.
- The EC event registry is `enhancedcelestials2core:lunar/event`; the translation key of an event is `lunar.event.<namespace>.<path>`.

## Checklist

- [x] Pure logic and the factors: `LunarMoodTest`, `LunarMoodsTest`, `EclipseSignalTest`, `HubAlmanacTest`.
- [x] The reflection against stand-ins with EC's names and signatures: `EcReflectTest` (cannot prove the real mod; `start` needs a `ServerLevel`, covered by the fake only).
- [x] GameTests with a fake EC: the real start-up links nothing without EC, no EC changes nothing, every mood, the schedule, the eclipse mirror, our chat line on a real eclipse start and end with and without EC (`CelestialsEclipseNoticeGameTests`), a broken source (dropped for good after 3 failures in a row), the bite, the forced shift with no other part's gate (the `-PpartsOff=species,artifacts` run), the rite cost (`CelestialsGameTests` and its siblings). An `EcReflect` stub with a missing member makes `create` fail and leaves no adapter (`EcReflectTest`).
- [x] `-PpartsOff=celestials`: `CelestialsPartOffGameTests` sees the part's hooks present on, absent off.
- [x] Every config key has an Admin row (`AdminEntries`, section `celestials`; `AdminCoverageTest`).
- [x] Hub shots in the three themes with stand-in events: `docs/superpowers/design/celestials/` (`*-events.png`, and `*-no-ec.png` for the unchanged look).
- [x] **Headless real-EC checks (no client was run):** a dedicated server with the four jars below loads a fresh world, logs `Enhanced Celestials knows our lunar event supernaturalbloodlines:lunar_eclipse: true` and no registry error; `runGameTestServer -PecJars=build/ecjars` (1067 tests) runs `CelestialsRealEcGameTests` against the real classes: the event is in EC's registry, and `start`/`stop` on a real forecast make our event the running one and end it. With the server (RCON, `/supernaturalbloodlines debug eclipse start` at night) our eclipse started with only our chat line.
- [ ] **Not seen in a real game:** two clients with EC 2.0.3.x and Default Lunar Events (core, add-on, CorgiLib, Data Anchor), one without EC. In particular: that the client forecast carries the schedule (the rings), that `requires_clear_skies` does not stop our mirrored event, the look of our `sky_light_color` (`c0403a`, a milder red than the first design's `8c0808` because the lightmap multiplies by it), and the chat notice.
- [ ] **EC announcing our mirrored event** (owner rule 9, deferred): start a mirrored eclipse with EC installed and see whether EC sends its chat text at the start and at the end (`removeLunarEvent`). Only if it does can our own line be skipped; until then both may show.
- [ ] NPC packs (plan 15) and the full-moon bosses (plan 17) do not exist yet: the Blood Moon pack chance and the super moon boss weight were removed (no key, no hook) and come back with them.
- [ ] The Home "Tonight: Blood Moon" block and a Guide paragraph (design 2b) are not built.

## Real jars used for the headless checks

| Jar | Where from | SHA-256 |
|---|---|---|
| `Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.4.jar` | https://cdn.modrinth.com/data/ugCWUHV9/versions/b5tyRnjL/Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.4.jar | `AFC0C3A1D142FCD8A1EA2E35D40A6C0C9E8B606FA4C84AD3BA18AB5311AF03DE` |
| `Enhanced-Celestials-2-Default-Lunar-Events-NeoForge-1.21.1-2.0.1.4.jar` | https://cdn.modrinth.com/data/19P0GlZk/versions/oqNjcnKK/Enhanced-Celestials-2-Default-Lunar-Events-NeoForge-1.21.1-2.0.1.4.jar | `085C3376DDE5A84C0885DB3AB7EEE09105D0D7DD0A176808D05ECF73ECAECF79` |
| `Corgilib-NeoForge-1.21.1-5.0.0.9.jar` (core asks `[5.0.0.9,)`) | https://cdn.modrinth.com/data/ziOp6EO8/versions/nqrTa84r/Corgilib-NeoForge-1.21.1-5.0.0.9.jar | `5E058A4FCB29A538821351081FCF10172FC707767756774AFED1BF0E1953C7E3` |
| `Data_Anchor-neoforge-1.21.1-2.0.0.18.jar` (core asks `[2.0.0.18,)`) | https://cdn.modrinth.com/data/z2XEADmE/versions/WwTx4TaQ/Data_Anchor-neoforge-1.21.1-2.0.0.18.jar | `53828CB3371672E7306795F9C5B640FD37FF79067CE715F2922B5A01530BC5CB` |

Put them in a folder **inside the project** (for example `build/ecjars`: a path with a drive colon is mangled by PowerShell when passed through a script) and run `./gradlew runServer -PecJars=build/ecjars` or `runGameTestServer -PecJars=build/ecjars`. EC is All Rights Reserved: the jars are never committed. The server needs `eula=true` in `run/`; stop it with `stop` on the console (or RCON).

## Things to know

- **The datapack files load regardless of the part switch**: the harvest tag (`enhancedcelestials2defaultlunarevents:harvest_moon_crops`) and the `lunar_eclipse` event are data, so switching the part off with EC installed leaves them in place (inert only because nothing asks for the event).
- **Before switching the part off or removing the mod, end the eclipse or wait for dawn.** A mirrored EC event that is running keeps running with nobody to remove it; it ends by itself at the end of its day. **Removing the mod while our event is the running one** (verified on a headless server: a world saved mid-eclipse, loaded with our datapack files removed): the server starts and plays, but EC logs `Error loading saved data: dataanchor_saved_tracked_data ... Failed to get element supernaturalbloodlines:lunar_eclipse` and **drops its saved forecast** (the schedule is rolled again); nothing else breaks. To avoid it, end the eclipse (`/supernaturalbloodlines debug eclipse stop`) before removing the jar.
- **The Harvest Moon doubling is unverified.** The tag is the one Default Lunar Events reads for its crop drops (javap), but a doubled drop has not been seen in a game with our crops.
- **`-PecJars=<folder>`** takes the four jars of the table above (all checked: the first two are the ones read with javap).
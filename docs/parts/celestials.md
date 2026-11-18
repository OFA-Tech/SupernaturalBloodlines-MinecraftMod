# Part: celestials

Requires: **no mod**. Enhanced Celestials 2 (EC) is optional and found at runtime (`ModList.isLoaded("enhancedcelestials2core")`); without it the part installs an empty lunar source and every factor is 1, so the game plays as before. Package: `net.ofatech.supernaturalbloodlines.parts.celestials`. Config key `celestials`, file `supernaturalbloodlines-celestials.toml`. Plan: `docs/superpowers/plans/2026-11-18-celestials.md`. Spec: `docs/superpowers/specs/2026-11-17-enhanced-celestials-compat.md`.

**Mixin budget: 0 of 3** (see `docs/mixins.md`).

## The rule

An EC event never replaces the real moon. `MoonRule.isFullMoon*`, the rites' "needs the full moon" check, the Werewolves forced shift and the Werewolves `Helper` keep reading the real phase and time. An EC event is a second layer, the **lunar mood** (`core/moon/LunarMood`: `NONE`, `BLOOD`, `BLUE`, `HARVEST`, `ECLIPSE`, plus a `superMoon` flag), read through `LunarMoods` (`core/moon`). A source that throws gives "no mood", never an exception on a tick.

## What each event does (all factors are config, 1 = off)

| EC event | Effect | Key |
|---|---|---|
| Blood Moon (any phase) | a vampire's bite gives x1.25 blood (rounded half up, an emptied bite stays empty). NPC werewolf packs at half chance: **a hook only** (`LunarMoods.packChanceFactor`), nothing reads it yet | `blood_moon_bite_bonus`, `blood_moon_pack_chance_factor` |
| Blue Moon (any phase) | every rite costs x0.8 altar power (the species part's `RiteCost.begin`). It never lets a full-moon rite run on another night | `blue_moon_rite_cost` |
| Harvest Moon | our crops (vervain, the two wolfsbane strains) are in EC's item tag `enhancedcelestials2defaultlunarevents:harvest_moon_crops` (a datapack file, `required: false`), so they drop x2 | none |
| any super moon (full moon only) | Rite of the Moon, Silver Moon and Immortality cost x0.75, on top of the Blue Moon factor. Boss weight x2: **a hook only** (`LunarMoods.bossWeight`) | `super_moon_rite_cost`, `super_moon_boss_weight` |
| Super Blue Moon | the Werewolves forced shift is skipped for everybody (`ForcedShift.addForcedSpare`); the shift at will still follows `moon_bound_werewolves` | `super_blue_skips_forced_shift` |
| our lunar eclipse | see below | existing `lunar_eclipse_chance` |

## The eclipse mirror

Our roll stays the source of truth (`LunarEclipse`, 3%, its SavedData, its gameplay). When it starts, `EclipseSignal` tells the part, which asks EC to start our event `supernaturalbloodlines:lunar_eclipse` tonight (`setLunarEventTonight`); when it ends, EC's event is removed (`removeLunarEvent(0)`). The event is a datapack file of ours (`data/supernaturalbloodlines/enhancedcelestials2core/lunar/event/lunar_eclipse.json` and four modifiers: sky light, moon colour, name colour, the two chat texts). EC then draws the moon colour and light, plays the notice (**so with EC our own chat line is not sent**), and the hub's almanac can name it. If EC already runs another event the mirror is skipped, our chat line is sent and the gameplay still runs.

Client: `EclipseClient` keeps its fog tint and its red sky disc always, and skips only its moon disc when this client's own EC forecast says the running event is ours (`LunarMoods.ecDrawsEclipse`). A client without EC on a server with EC still gets all of ours.

## The hub

The almanac of the three Path pages (Grimoire `PathPage`, Blood Moon `BloodMoonPathPage`, Tensura `StagePathPage`) draws a ring in the event's name colour on the day's moon (tooltip: "Blood Moon in 3 days.") and a line "Blood Moon in 3 days." under the full-moon line. The data is `LunarMoodClient` (read once per game tick). Without events the pages are exactly as before.

## Layout

- `core/moon/` (pure but `Level` in the source interface): `LunarMood`, `LunarSource`, `LunarMoods` (the facade and the `Tuning` numbers), `EclipseSignal`.
- `parts/celestials/`: `CelestialsPart` (init, install), `CelestialsConfig`, `EcAdapter` (what the part needs), `EcReflect` (the only class that names EC types, by reflection), `EcSource`, `BloodMoonBite` (links Vampirism), `SuperBlueMoon` (links Werewolves), `gametest/` (`FakeEc` and the tests).
- Hooks in other parts: `RiteCost` (species), `ForcedShift.addForcedSpare` (shared), `LunarEclipse.changed` and `EclipseClient` (the notice and the moon disc), `HubAlmanac`/`AlmanacEvents` (hub).

## Checked against the EC jars (core 2.0.3.4, Default Lunar Events 2.0.1.4, by javap)

- `registerLunarEventChanged(LunarEventChangeListener)`; the listener is `onLunarEventChanged(ServerLevel, Holder previous, Holder new)`: `LunarForecast.tick` calls `notifyEventSwitched(lastTickEvent, current)`, which fires `(level, previous, new)`. `EcReflectTest` pins that order.
- An event instance runs from `day*24000 + night_start_time (12500)` (or "now" for `setLunarEventTonight`) to `day*24000 + 23999`, the end of the day number. Our eclipse ends at 23216, so the mirror is removed at our end (`removeLunarEvent(0)` shortens the active instance to "now").
- `setLunarEventTonight` has no clear-sky check (only the command and the scheduler have); it throws on the client.
- EC's client does **not** tint the fog or the sky dome: its mixins change only the moon colour, texture and size and the lightmap's sky colour. So our fog and sky tint stay ours.
- The EC event registry is `enhancedcelestials2core:lunar/event`; the translation key of an event is `lunar.event.<namespace>.<path>`.

## Checklist

- [x] Pure logic and the factors: `LunarMoodTest`, `LunarMoodsTest`, `EclipseSignalTest`, `HubAlmanacTest`.
- [x] The reflection against stand-ins with EC's names and signatures: `EcReflectTest` (cannot prove the real mod; `start` needs a `ServerLevel`, covered by the fake only).
- [x] GameTests with a fake EC: no EC changes nothing, every mood, the schedule, the eclipse mirror and the single notice, a broken source, the bite, the forced shift, the rite cost (`CelestialsGameTests` and its siblings).
- [x] `-PpartsOff=celestials`: `CelestialsPartOffGameTests` sees the part's hooks present on, absent off.
- [x] Every config key has an Admin row (`AdminEntries`, section `celestials`; `AdminCoverageTest`).
- [x] Hub shots in the three themes with stand-in events: `docs/superpowers/design/celestials/` (`*-events.png`, and `*-no-ec.png` for the unchanged look).
- [ ] **Not seen in a real game:** two clients with EC 2.0.3.x and Default Lunar Events (core, add-on, CorgiLib, Data Anchor), one without EC. In particular: that the client forecast carries the schedule (the rings), that `requires_clear_skies` does not stop our mirrored event, the look of our `sky_light_color` (`c0403a`, a milder red than the first design's `8c0808` because the lightmap multiplies by it), and the chat notice.
- [ ] NPC packs and the plan 17 bosses do not exist yet: `packChanceFactor` and `bossWeight` have no reader.
- [ ] The Home "Tonight: Blood Moon" block and a Guide paragraph (design 2b) are not built.

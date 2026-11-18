# Enhanced Celestials 2 compatibility (design, 2026-11-17)

**Update 2026-11-18: built** (plan `docs/superpowers/plans/2026-11-18-celestials.md`, checklist `docs/parts/celestials.md`). The owner approved all eleven recommendations of (f). Three "unverified" items are now checked against the jars: the listener order is `(ServerLevel, previous, new)`; an event instance ends at `day*24000 + 23999` (our eclipse ends at 23216, so the mirror is removed at our end); EC's client does **not** tint the fog or the sky dome (only the moon and the lightmap), so our fog and sky tint stay and only our moon disc is skipped. The part has no required mod (so it can be tested without EC).

Status (original text): research and proposal. Nothing is built, nothing is added to the build. Every number below was read from the 1.21.1 NeoForge jars (javap and their datapacks), not from the pages; the things I could not check are marked **unverified**.

## 0. What the mod is (research)

**Enhanced Celestials 2** (Corgi Taco) is a library plus add-ons, not one mod:

| Piece | 1.21.1 NeoForge file (latest, 2026-11-05) | Adds |
|---|---|---|
| **Core** `enhancedcelestials2core` | `Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.4.jar` (Modrinth `ugCWUHV9`, CurseForge project 1621796; the CurseForge "5.0.0.x" files are the 26.3 builds, 1.21.1 is the 2.0.x line) | The scheduler, the datapack loader, the sky/moon rendering, the modifiers, commands. **No events at all** except `enhancedcelestials2core:default` ("Normal Moon") |
| **Default Lunar Events** `enhancedcelestials2defaultlunarevents` 2.0.1.4 (needs core 2.0.3.1+) | | `blood_moon`, `blue_moon`, `harvest_moon`, `super_moon`, `super_blood_moon`, `super_blue_moon`, `super_harvest_moon` |
| Shader Support, plus about 12 community add-ons (Horde Moon, Hunter's Moon, Chaos Moon, ...) | | not needed |

- **Dependencies of core:** NeoForge 21+, Minecraft 1.21.1+, **CorgiLib >= 5.0.0.9**, **Data Anchor >= 2.0.0.18**. It declares the original `enhancedcelestials` **incompatible**.
- **Licence:** "All Rights Reserved" (CurseForge "Custom License", Modrinth ARR). The GitHub repo `CorgiTaco-MC/Enhanced-Celestials-2` holds **no source**, only issues and the experimental datapack editor jars. So: no bundling, no shading, no copying of its files; the API can only be read from the jar.
- **Age:** about 3 months old (first file 2026-07-23), a dozen releases since, so the API can still move.
- **Events are not full moons.** `blood_moon`, `blue_moon` and `harvest_moon` can happen on **any** of the 8 phases (weight 10 each, at least 4 nights apart). The three `super_*` events and `super_moon` are **phase 0 only** (weight 5, at least 20 nights apart). Overworld only. One event at a time. The scheduler rolls a 100-day year with 3 to 98 days between events and `requires_clear_skies: true`; I did not run it, so the real frequency is **unverified** (it looks like one event every few weeks).
- **How it is configured:** no TOML. Everything is datapack JSON (`dimension_settings`, `event_probability`, `event`, `event_modifier`), edited by the datapack or by its in-game config screen. Commands exist (set, schedule, forecast, dump modifiers; names from its lang keys).
- **Moon phase and day length:** **not changed.** Its mixins (14 common, 5 client) touch spawning, drops, anvil, enchanting, beacon, sleeping, siege, the lightmap, the sky renderer, and `TimeCommandMixin` (it rewrites `/time` so the schedule stays in step). None touches `getMoonPhase` or `getDayTime`. `day_length` (24000) is a setting it reads. So vanilla phases, `Level.getDayTime()` and Werewolves' time checks behave as today.

### The default events (what each one does)

| Event | Phases | Modifiers |
|---|---|---|
| Blood Moon | any | red sky and moon, monster spawns x2.25, mobs on the surface, spawn distance 64, **no sleeping**, village siege chance 0.5, notice in chat |
| Blue Moon | any | Luck for everyone, XP x2, anvil and enchanting cost down, beacon radius up |
| Harvest Moon | any | crop drops x2 (item tag `enhancedcelestials2defaultlunarevents:harvest_moon_crops`, plus wheat) |
| Super Moon | 0 | bigger moon, slimes spawn everywhere |
| Super Blood / Blue / Harvest | 0 | the matching event plus a bigger moon (40) |

Modifier types a datapack can use (`enhancedcelestials2core:<name>`): `sky_light_color`, `moon_texture_color`, `glow_color`, `moon_size`, `moon_texture`, `sound_track`, `text_components`, `name_color`, `spawn_category_multiplier`, `mob_effects`, `block_sleeping`, `disable_biome_spawn_settings`, `force_surface_spawning`, `slimes_spawn_everywhere`, `mob_spawn_settings`, `block_item_drop`, `entity_drop`, `anvil_cost`, `enchantment_cost`, `experience`, `beacon_radius`, `village_siege_probability`, `mob_spawn_distances`, `mob_spawn_equipment`, `existing_mob_equipment`. Spawn rules: `min_nights_between`, `valid_moon_phase`.

### The public API (how another mod uses it)

All in `dev.corgitaco.enhancedcelestials2core`. `LunarForecast` extends Data Anchor's `SyncedLevelTrackedData`, so it **exists on both sides** (the client has the synced state; the hub can read it).

- **(a) Which event is active now.** `EnhancedCelestials.lunarForecastWorldData(Level)` returns `Optional<LunarForecast>`. Then:
  - `isEventActive()` is `currentLunarEventHolder() != default event && getBlend() > 0.01`.
  - `currentLunarEventHolder()` is a `Holder<LunarEvent>`; test it with `holder.is(ResourceLocation)` or a tag of the registry `enhancedcelestials2core:lunar/event`.
  - Ahead: `upcomingEventsByDaysInAdvance()` (a `Long2ObjectRBTreeMap<Holder<LunarEvent>>`, key = days from today), `scheduledEventsByDay(long)`, `getNextScheduledLunarEvent()`, `getForecastComponent()` (a ready text), `currentDay()`, `maxScheduledDaysInAdvance()`.
  - Event data: `LunarEvent.getNameColor()`, `getSkyLightColor()`, `getMoonTextureColor()`, `getMoonSize()`, `startNotification()/endNotification()`, `LunarEvent.getTranslationKey(holder)`, `getSpawnMultiplierForMonsterCategory(...)`.
- **(b) Start and end.** `EnhancedCelestialsEvents.registerLunarEventChanged(LunarEventChangeListener)`; the interface has one method `onLunarEventChanged(ServerLevel, Holder<LunarEvent>, Holder<LunarEvent>)` (server side only, fired from `LunarForecast.tick` through `notifyEventSwitched`). The order is probably (level, previous, new); **unverified**. A "start" is a new holder that is not the default; an "end" is the default coming back. It is a plain static listener list, not a NeoForge event.
- **(c) Registering your own event by datapack** (no code): files in your namespace, read only when EC is installed:
  - `data/<ns>/enhancedcelestials2core/lunar/event/<id>.json`: `{"modifiers": ["<ns>:<modifier id>", ...]}`
  - `data/<ns>/enhancedcelestials2core/lunar/event_modifier/<id>.json`: `{"type": "enhancedcelestials2core:sky_light_color", "settings": {"color": "8c0808"}}`
  - `data/<ns>/enhancedcelestials2core/lunar/event_probability/<id>.json`: `{"lunar_event": "<ns>:<id>", "dimensions": {"minecraft:overworld": {"priority": 0, "spawn_requirements": {"weight": 5, "rules": [{"type": "enhancedcelestials2core:valid_moon_phase", "settings": {"valid_moon_phases": [0]}}, {"type": "enhancedcelestials2core:min_nights_between", "settings": {"min_nights": 20}}]}}}}`
  - optional tags `data/<ns>/tags/enhancedcelestials2core/lunar/event/<tag>.json`; our item ids can also be **added to its item tag** `harvest_moon_crops` (tags merge, `"required": false`).
  - By code: `LunarForecast.insertLunarEvent(holder, daysAhead)`, `setLunarEventTonight(holder)`, `removeLunarEvent(long)`, `removeLunarEvents(Predicate)`.

## 1. How our mod uses the moon today

- **Rule (pure):** `core/moon/MoonRule`: `isFullMoon(hasFixedTime, phase)` (phase 0, not in the Nether or End) and `isFullMoonNight` (also time of day 12786 to 23216, Werewolves' window).
- **Werewolves mod:** its own `Helper.isFullMoon` and `isNight` read **`Level.getDayTime()`** (`% 192000` and the 12786 to 23216 window), not `getMoonPhase`. **Werewolves has no eclipse** (only the advancements `bad_moon_rising` and `howling_moon`). The eclipse is entirely ours. Its forced full-moon shift is Werewolves' own `onUpdate`; we only cancel it through `ActionActivatedEvent` (`ForcedShift`, `MoonControl`, R96). We cannot raise or lower what Werewolves forces, only veto.
- **Users of the full moon:** rites (`FactionRiteFactory:182`, `SeveranceRiteFactory:75`: Moon, Immortality, Silver Moon, Severance need the night), skills (`SkillSpec.When.FULL_MOON_NIGHT`, `SkillEffects`), the HUD forced-shift warning (`MoonClock`, `LiveHud`), the hub (`HomePage:187`, `PathPage:95`, `BloodMoonPathPage:96`, `StagePathPage:101`, `TensuraHubTheme:184`, all through `HubAlmanac.phases/daysToFull`: "The full moon rises in N days"), the Guide chapter "moon".
- **The lunar eclipse:** `parts/species/pack/LunarEclipse` (SavedData `supernaturalbloodlines_eclipse`, one night number). At the first tick after dusk of a phase-0 night with someone online it rolls `lunar_eclipse_chance` (3%). While on: `FormBlocks`, `RankEffects` and `PackBonus` suppressors, chat messages `supernaturalbloodlines.eclipse.start/end`, `EclipsePayload` to clients, `/supernaturalbloodlines debug eclipse`. Client: `EclipseClient` tints the fog (`ViewportEvent.ComputeFogColor`), draws a red disc over the sky (`SKY_TINT 0x8C0808`, alpha 0.55) and a dark orange moon disc (`MOON_TINT 0xB4501E`) after `AFTER_SKY`.
- **Planned:** NPC werewolf packs on full-moon nights (bosses design 6, "plan 15"; there is no written plan 15 yet, hub IA open question 10), the Beast of Gevaudan Host (spawns on a full moon, 25%), plan 17 bosses (Klaus: full moon, Deucalion: full moon, Peter Hale: feral on full moons, Finn: **lunar eclipse** ritual). The hub IA wants Home "The world now" (phase, days to the full moon, an eclipse running or coming), and the UI plan notes "eclipse in 41 days" needs a schedule that reaches the client. **Today there is no eclipse schedule**: the roll happens at dusk, so only "tonight" can be shown.
- **Mixins:** `docs/mixins.md` budget is 3 per part. This work needs none.

## 2. Design

### 2.0 One rule first

**An EC event never replaces the real moon.** `MoonRule.isFullMoon*`, the rites, the Werewolves forced shift and the Werewolves `Helper` keep reading the real phase and time. EC events are a **second layer**, a "lunar mood" on top, because a blood moon can fall on a new moon and Werewolves cannot be told otherwise. Everything below reads the mood through one pure value in `core/moon` that is `NONE` when EC is absent.

```
core/moon/LunarMood      pure: enum Kind { NONE, BLOOD, BLUE, HARVEST, ECLIPSE } + boolean superMoon
core/moon/LunarSource    one-method SPI: Optional<LunarMood> now(Level), Optional<Upcoming> ahead(Level, days) ; default source returns NONE
```
The eclipse is always `ECLIPSE`, from our own `LunarEclipse`, with or without EC. Only BLOOD, BLUE and HARVEST (and the super flag) come from EC.

### (a) Mapping proposal

| EC event | Meaning for us | Config key (new `supernaturalbloodlines-celestials.toml`) |
|---|---|---|
| **Blood Moon** (any phase) | A "feral night". **NPC werewolf packs** (plan 15) may spawn as on a full moon, at half the chance, with their Alpha. Werewolf players: **no forced shift** (R96 stays: only the real moon forces). **Vampires** get more from blood: bite blood x1.25. Plan 17: Klaus and Deucalion may start on a blood moon at half weight. Our own mobs get EC's x2.25 for free. | `blood_moon_pack_chance_factor` 0.5, `blood_moon_bite_bonus` 1.25 |
| **Blue Moon** (any phase) | A "calm, thin-veil night". **Altar power cost of rites x0.8** (one more factor in the existing `RiteCostMixin` `@ModifyVariable`, so no new mixin) and, for the rarer rites below, the condition they ask for. A blue moon never lets a full-moon rite run on a non-full night. | `blue_moon_rite_cost` 0.8 |
| **Harvest Moon** (any phase) | Plants. **Zero code:** our datapack adds the wolfsbane strains, mountain ash, garlic and our other crops to the item tag `enhancedcelestials2defaultlunarevents:harvest_moon_crops`, so they drop x2. | none (tag) |
| **Super Moon / Super Blood / Super Blue / Super Harvest** (phase 0 only, so always a real full moon) | A "full moon at its strongest". Same as the matching event plus: NPC pack Alpha always present, plan 17 full-moon bosses at x2 weight, Rites of the Moon, Silver Moon and Immortality cost x0.75. **Super Blue** also skips the Werewolves forced shift for everybody (it is the one kind that coincides with a real full moon and is calm), which is a gift for players, so it is the one with an explicit switch. | `super_moon_boss_weight` 2.0, `super_blue_skips_forced_shift` true |
| **Our lunar eclipse** | See the next section. | existing `lunar_eclipse_chance` |

**New, optional rites** (not for the first release, noted so the hook is right): a rite factory field `requires_lunar_event` (an event id or tag, optional, EC only) lets future content ask for "a blue moon" (The True Cure, Rebekah) or "an eclipse" (Finn's linking ritual). Those rites are shipped behind a `neoforge:mod_loaded enhancedcelestials2core` condition, so without EC they do not exist, and no existing rite changes.

**Should our eclipse become an EC lunar event?** Yes, as a **mirror**, not as the source of truth:

- **Option B (recommended): our roll stays authoritative.** `LunarEclipse` keeps its dusk roll, its 3% config, its SavedData and all its gameplay (forms, ranks, pack bonus). When it starts, the `celestials` part calls `forecast.setLunarEventTonight(ours)` (and `removeLunarEvent` when it ends), so **EC draws the sky, the moon colour, the notification and the soundtrack**. Our event is the datapack file `supernaturalbloodlines:lunar_eclipse` with `sky_light_color 8c0808`, `moon_texture_color b4501e`, `name_color`, `text_components` using our two lang keys, no spawn modifiers. When EC is present and showing it, `EclipseClient` skips its sky and moon discs and `LunarEclipse.changed` skips the chat broadcast (EC sends the notification); the fog tint stays ours until someone confirms in game that `sky_light_color` also covers the fog (EC's `ECWorldRenderer` reads it; whether it tints the fog is **unverified**).
- **Option A (later, if wanted): EC schedules it.** An `event_probability` file with `valid_moon_phase [0]` and a weight, and we read `isEventActive && holder is ours`. It gives a real forecast ("eclipse in 41 days"), but EC's weights and 3 to 98 day gaps cannot express "3% of full moons", it competes with blood moon for the same night, and it makes the rules depend on EC. Not recommended for the first release.
- **Forecast without EC (separate change, not part of this):** to show "eclipse in N days" everywhere, the roll could become a seeded function of (world seed, night number), which keeps 3% and gives a forecast with no EC. That changes today's behaviour slightly (same rate, different draws), so it is its own task.

### (b) The hub calendar

Today: `HubAlmanac.phases(today, 6)` draws six moon icons and the line "The full moon rises in N days". With EC and at least one event scheduled:

- **Each of the six day icons** gets a small ring in the event's `name_color` when `upcomingEventsByDaysInAdvance()` has an entry for that day (key = days ahead), with the name in the tooltip (`LunarEvent.getTranslationKey`). A super event on a phase-0 day sits on the full moon icon.
- **Second line under the full-moon line:** "Blood Moon in 3 days" (next scheduled event), same style in the three themes (Grimoire ink note, Blood Moon sub-label, Tensura info row).
- **Home "The world now":** "Tonight: Blood Moon" in its colour while `isEventActive()`, ahead of the full-moon line. Our eclipse keeps its own line as today.
- **Guide chapter "moon":** one extra paragraph and one extra line in the Guide's mood table, **only when EC is loaded**.
- The hub reads a per-tick cache (`LunarMoodClient`), never the forecast per frame. The client has the forecast through Data Anchor sync; if it is missing or an exception happens the cache says "no events" and the hub looks as it does today.
- Not shown: events further than the almanac's six days (the Path page has room for six only); a "calendar of the year" page is a possible later task.

### (c) Without EC

Everything keeps working exactly as today, by construction:
- `LunarMood.NONE` is the default; `MoonRule`, the rites, `ForcedShift`, `MoonClock`, `HubAlmanac` and their tests are not edited, only **read** `LunarMood` where a mapping above adds a factor, and a factor of NONE is 1.
- `LunarEclipse` and `EclipseClient` behave as now; the "skip our discs" branch needs a true answer from the bridge, which is false when the part is off.
- No class of EC is loaded unless the part is on: the only class that names EC types is `EcBridge` (reflection), created by the `celestials` part's init.
- The data files for the eclipse event sit in our jar and are ignored when EC is absent (vanilla loads only the registries it knows; the `enhancedcelestials2core/` folder is read only by EC).
- Tests: unit tests for `LunarMood` and the factors; a fake `LunarSource` for the hub model and for the rite cost; the GameTest runtime has no EC.

### (d) Integration approach

- **`neoforge.mods.toml`:** an optional dependency on `enhancedcelestials2core`, `versionRange="[0,)"` (the project already found that an optional mod present but out of range **stops the game**, so we do not pin it; `OpenPartiesAndClaims` is declared the same way), `ordering="AFTER"`, side BOTH. Add `"enhancedcelestials2core": "2.0.3"` to `LinkedMods.TESTED`, so a newer EC loads with a warning. No dependency on CorgiLib or Data Anchor: they are EC's.
- **New part `celestials`** (switch `parts.celestials`, default on; `requiredMods()` = `enhancedcelestials2core`; no other part required; config `supernaturalbloodlines-celestials.toml`). The pure `LunarMood`/`LunarSource` live in `core/moon` so species, supernatural and hud read them without importing the part. The part registers its `LunarSource`, the `registerLunarEventChanged` listener, and the eclipse mirror (the existing `LunarEclipse.changed` gets one core-level callback list, `EclipseSignal`, so species does not import celestials).
- **Reflection, not `compileOnly`.** The surface is about eight members (`EnhancedCelestials.lunarForecastWorldData`, `LunarForecast.isEventActive/currentLunarEventHolder/upcomingEventsByDaysInAdvance/setLunarEventTonight/removeLunarEvent`, `EnhancedCelestialsEvents.registerLunarEventChanged` with a `java.lang.reflect.Proxy` for the listener interface), resolved once with `MethodHandles` in `EcBridge`. Reasons: ARR licence and no public source or maven; `LunarForecast` extends Data Anchor and its signatures use CorgiLib types, so `compileOnly` needs three jars on the classpath; the API is three months old and a changed signature should log one warning and switch the source off, not throw `NoSuchMethodError` on the server thread. If a member is missing, the part logs once and the mood stays NONE (the project already does this for Open Parties and Claims).
- **Mixin budget: 0.** The Blue Moon rite cost reuses the existing `RiteCostMixin` hook (a factor, not a new injection). EC's own mixins do not overlap ours (EC: `LivingEntity`, `Mob`, `Player`, `Entity`, `NaturalSpawner`, `Block`, menus, `TimeCommand`, `LevelRenderer`, lightmap; we have none of those).
- **Dev and tests:** EC is **not** added to `localRuntime` for now. A Gradle property (off by default) could add it with Data Anchor and CorgiLib for a manual check later.

### (e) Risks

1. **Licence:** ARR, so never bundle, copy its files or its textures; the wiki says "optional, install from CurseForge or Modrinth".
2. **Four jars for the player:** core, Default Lunar Events, CorgiLib, Data Anchor. EC core alone has no events (only "Normal Moon"), so "EC installed without Default Lunar Events" must give mood NONE and still show our eclipse. Document it.
3. **API drift:** young, no public source, 26.x files carry a different version line. Mitigated by reflection, fail-soft, `LinkedMods` warning, and one manual check per EC release.
4. **Semantic mismatch:** EC events are not full moons; `requires_clear_skies` may stop an event on a rainy night (unverified for the API call `setLunarEventTonight`, it is enforced for the command); EC's night window starts at 12500 (`night_start_time`) while ours is 12786 to 23216 (when EC's event ends is **unverified**). The rule "the real moon drives the rules" keeps the gameplay safe; only the look can be a few hundred ticks off.
5. **One event at a time:** if our eclipse starts while a blood moon is running (or the reverse), only one can be the EC event. Rule: our eclipse gameplay always runs; the mirror is skipped when EC already has an event active, and then our own tint draws (the fallback stays in the client).
6. **Double visuals and double chat:** our discs and notice must be skipped **only** when this client has EC **and** the active EC event is ours. A server with EC and a client without it must still get our tint, so the decision is made on the client from the client's own forecast, not from a server flag.
7. **Balance:** blood moon x2.25 monsters, no sleeping and extra NPC packs can be very hard together; every factor is a config key and the pack-chance factor defaults to 0.5.
8. **Performance:** the hub must not read the forecast per frame (per-tick cache); `upcomingEventsByDaysInAdvance` is small.
9. **Untested in game:** I did not run Minecraft; nothing here was seen working. The first build needs a manual two-client check with EC and the Default Lunar Events, plus one without EC.
10. **Tests cannot prove the bridge:** the GameTest runtime has no EC, so only the pure mood logic and the fake source are automated.

### (f) Open questions for the owner (with my recommendation)

1. **Is an EC event only ever a second layer, never a replacement of the full moon for rites and the forced shift?** Recommend: yes (the rule in 2.0). A blood moon on a new moon will not make a werewolf shift.
2. **Eclipse: mirror (B) or let EC schedule it (A)?** Recommend B now; A only if you want an eclipse forecast, and then through our own seeded roll so it also works without EC.
3. **Do the `super_*` events (always a real full moon) buff the full-moon systems?** Recommend: only cheaper rites, boss weight x2 and an Alpha always in NPC packs; never a stronger forced shift.
4. **Super Blue Moon skips the forced shift for everyone?** Recommend: yes, default on, one config key. It gives players one safe full moon.
5. **Vampire blood bonus on a Blood Moon (bite blood x1.25)?** Recommend: yes, default on, and tell the Guide.
6. **Should Blue Moon let you perform a full-moon rite on a non-full night?** Recommend: no. It only lowers the cost (x0.8), and unlocks new EC-only rites later.
7. **Add our crops to `harvest_moon_crops`?** Recommend: yes (zero code): wolfsbane strains, mountain ash, garlic, our other crops.
8. **Is the new part `celestials` (default on) the right place, with the pure `LunarMood` in core?** Recommend: yes; it keeps species, supernatural and hud free of EC imports.
9. **Use EC's chat notice for the eclipse when EC is there, and drop ours?** Recommend: yes, to avoid two lines. Our line stays without EC.
10. **Add EC to the dev runtime for a manual check?** Recommend: a Gradle property, off by default, so CI and GameTests stay as they are.
11. **Any pack-maker promise?** Recommend: none yet: "optional, tested with EC core 2.0.3.x and Default Lunar Events 2.0.1.x on 1.21.1" in the wiki Known Limits, with the manual check pending.

## 3. Build order (when approved)

1. `core/moon/LunarMood`, `LunarSource`, unit tests, config keys (no EC, no behaviour change).
2. `EcBridge` (reflection) and the `celestials` part, mods.toml entry, `LinkedMods`, `docs/parts/celestials.md`, `docs/mixins.md` line "celestials (0 of 3)".
3. Eclipse mirror: datapack event, `EclipseSignal`, client skip.
4. Hub: almanac rings and "in N days" line, Home block, Guide paragraph.
5. Mapping: harvest tag, blue-moon rite cost, blood-moon bite bonus; NPC pack and boss factors when plans 15 and 17 exist.
6. Manual two-client check with and without EC; wiki and changelog.

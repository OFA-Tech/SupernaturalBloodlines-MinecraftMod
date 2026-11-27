# Plan 23 Task 0: the hooks the playtest fixes rely on

Recorded on `feature/plan-23-proofs` from `origin/main` `c41686f5` (20d merged), 2026-11-26. Proofs that run in a GameTest
log a `PLAN23 Pn` line; the lines quoted below are copied from those logs. Static reads are `javap -c -p` of the
owner's pack jars, copied read-only from the CurseForge instance (`mods`, 72 files) to a scratch folder.

## Versions and command lines

- Jars read: `tensura-neoforge-2.0.1.5`, `manascore-neoforge-4.0.0.2` (inner `manascore-race`), `Vampirism-1.21-1.10.13`,
  `Werewolves-1.21-2.0.3.4`, `Tensura_Horizons-0.31`, `ascension-3.1.0`, `mysticism-neoforge-2.2.1`,
  `Enhanced-Celestials-2-Core-NeoForge-1.21.1-2.0.3.4`, NeoForge `21.1.265` (universal).
- `javap -c -p -cp <jar> <class>` with the JDK 21.0.11 `javap`; `-v` for bootstrap methods (lambdas of `TensuraKeybinds`).
- GameTests: `./gradlew runGameTestServer` (default), `-PpartsOff=species` (P4), and
  `-PaddonJars=<pack-notrainer copy>` (P6 addons, P12). Classes: `parts/vampirismtensura/gametest/RaceHookGameTests`,
  `parts/speciestensura/gametest/HumanLikeProbeGameTests`, `parts/vampirismtensura/gametest/VampirismMenuHookGameTests`.

## Counts (Step 1)

| What | Plan said | Main today (20d merged) |
|---|---|---|
| `AdminEntries.ALL.size()` | 277 | **293** (`259 + 18 + 16`) |
| `AdminCommands.ALL.size()` | read it | **79** |
| `LadderNet.PROTOCOL` | "18" | **"19"** |
| `AdminSnapshotPayload.MAX_ROWS` | 320 | 320 |
| `SnapshotIdsWireTest.GOLDEN_PROTOCOL` | "18" | **"19"** |
| Rule ids above R346 | none | none (R347 to R362 stand) |

Plan 23 adds 10 server rows and 2 player rows: 305, under `MAX_ROWS` 320.

Baseline (Step 3) before any plan-23 test: unit tests **3322**, GameTests default **2796**. The other setups at the same
code (the 20d matrix on `741cdc86`, whose merge is `c41686f5`): nowolves 2256, nolinked 30, off-species 572,
off-artifacts 2632, off-speciesTensura 2551, off-supernatural 2646, off-supernaturalTensura 2786, off-hud 2596,
off-celestials 2780, off-bosses 2425, off-species-artifacts 419, off-evolutionRites 2665, addons19a 2796,
addons19b 2796, real-ec 2798.

## Step 2: what later plans change

20d is merged. Its human-like reads on main: `BrotherhoodFit` and `GroupTypes.Leader` take a `humanLike` fact; Task 18's
source scan must find them. 20e (plan only) adds `JoinRules`, which will read the same fact; re-record when it merges.

## The proofs

### P1: a pick, our own write, an evolution and the race command, as `SET_RACE` sees them (proven, sharpened)

`ManasCore RaceStorage.setRace(instance, evolution, notify, message)` posts `SET_RACE(old, entity, new, evolution,
Changeable.of(notify), Changeable.of(message))`. Seen by a probe:

    PLAN23 P1 seen [tensura:human>tensura:elf flag=false changeable=true message=true reincarnationMenuOpen=true,
      tensura:elf>tensura:dwarf flag=false changeable=false message=false reincarnationMenuOpen=false,
      tensura:human>tensura:enlightened_human flag=true changeable=false message=false reincarnationMenuOpen=false,
      tensura:enlightened_human>tensura:merfolk flag=false changeable=false message=false reincarnationMenuOpen=false]
      command=tensura edit race @s set tensura:merfolk

- (a) the pick (`ReincarnationMenu.setRace(player, race, resetExistence, grantUnique)`, which calls
  `setRace(instance, false, true, "tensura.command.race.edit")`): flag false, changeable **true**, a message, and the
  reincarnation menu is the open container.
- (b) our own `setRace(id, false)`: false, false, no message, no menu.
- (c) `Races.evolveRace`: flag **true**, false, no message.
- (d) `/tensura edit race <targets> set <race>` (the real spelling; `RaceCommand.set` calls `setRace(race, false)`):
  **exactly the shape of our own write**.
- The first-login random pick is the same call: `clickMenuButton` picks from `getRandomRacePool()` and calls the same
  static `setRace` (`ReincarnationMenu.clickMenuButton` bytecode 89-173), so it needs no marker of its own.
- **Decides Task 14:** the open `ReincarnationMenu` separates a pick from every admin or mod edit, as the plan wanted.
  The changeable flag is a second, independent marker (true only for a pick). Neither the flag nor the message tells
  the command from our own writes, so "never map an admin edit" cannot be done by flags; the menu check is required.

### P2: a race a later listener refused is unchanged next tick (proven)

    PLAN23 P2 setRace refused=false control=true now=tensura:human nextTick=[tensura:human, tensura:dwarf]

`setRace` answers false on a refusal and the race is the old one now and at the next `ServerTickEvent.Post`; the
unrefused control shows the new race. `RaceSettled`'s premise holds.

### P3: a Ghoul's evolution to Vampire is refused today (proven, the finding stands)

    PLAN23 P3 ghoul evolve->vampire=false race=tensura:ghoul told=true level2=false level=1; level-1 vampire on tensura:vampire faction changes=[] control=[1>2]

Refused with `turning.ghoul_no_evolve`; `GhoulGates.refuseLevelUp` keeps a Ghoul at level 1; `raiseToRaceFloor` does
not touch a level-1 vampire on `tensura:vampire` (the same probe sees a real level change of that player, the control `1>2`, so the empty list means nothing changed). Task 14 turns the first line around.

### P4: species off leaves a pick alone (proven, `-PpartsOff=species`)

    PLAN23 P4 mysticism:direwolf not registered; tensura:beastfolk race=tensura:beastfolk faction=null; tensura:ghoul race=tensura:ghoul faction=null

### P5: undead and no-blood races and the Werewolves (proven for R103, one assumption disproven)

    PLAN23 P5 tensura:ghoul ... canJoin=false rawSetFactionAndLevel=true ... awakenHuman=supernaturalbloodlines.rite.join_denied stateUnchanged=true faction=null;
      tensura:slime ... canJoin=false rawSetFactionAndLevel=true ...; tensura:wight ... canJoin=false rawSetFactionAndLevel=true ...

- `CanJoinFaction` denies all three (R103 holds at `canJoin`), and `GeneHandler.awakenHuman` refuses with
  `rite.join_denied` and leaves nothing behind.
- **Disproven:** `IFactionPlayerHandler.setFactionAndLevel` (so `WerewolfFaction.join`) does **not** post
  `CanJoinFaction`: the raw join succeeds for a ghoul, a slime and a wight. Task 14 must call the seam only through
  `awakenHuman` (which asks `joinAllowed` first) or ask `canJoin` itself; a raw join would break R103.

### P5b: the awakening needs the gene (new finding)

    PLAN23 P5b beastfolk human without the gene: awakenHuman(GENE_AWAKENED)=supernaturalbloodlines.refusal.no_gene faction=null

`PackRules` refuses `GENE_AWAKENED` for a player without the werewolf gene. **Task 14 cannot be
`awakenHuman(p, GENE_AWAKENED)` as written**: a direwolf or Beastfolk pick by a gene-less player would always be
refused. Task 14 needs its own pack event (Omega, born, no gene needed) or to grant the gene first; that is a rules
decision for the PR-4 author, recorded here, not taken.

### P6 (G): the human-like set on the running jars (proven, two findings)

Without addons:

    PLAN23 P6 human_like tag (without ours): [tensura:beastfolk, tensura:dwarf, tensura:elf, tensura:human, tensura:merfolk]
    PLAN23 P6 closure null-entity=24 fake-player=24 thrownNull=[] thrownFake=[] differ=[]
    PLAN23 P6 naive closure [... tensura:divine_vampire ... tensura:vampire, tensura:vampire_lord, tensura:vampire_overcomer]
    PLAN23 P6 human-like races in #no_blood=[tensura:divine_beast, tensura:divine_dwarf, tensura:divine_elf, tensura:divine_fish, tensura:divine_human,
      tensura:dwarf_saint, tensura:elf_saint, tensura:human_saint, tensura:merfolk_saint, tensura:spirit_beast] in #spiritual=[the same ten]

- The tag is exactly the five base races; `getNextEvolutions(instance, null)` and with a fake player give the same graph
  and nothing throws; the naive closure reaches the whole vampire line (the plan's finding holds).
- **Finding: the plan's filter is wrong.** `#tensura:no_blood` contains `#tensura:spiritual`, and the saint and divine
  tiers of all five lines are spiritual: filtering `no_blood` drops ten races of today's `HumanLikeLine.EVOLVED`.
  The filter that reproduces today's list exactly is **closure minus `#tensura:undead`, minus `#tensura:daemon`, minus
  the vampire line** (no `no_blood`). With it the closure equals `HumanLikeLine.EVOLVED`'s Tensura ids both ways
  (`theFilteredClosureEqualsTheHardCodedEvolvedList`).

With the pack's addons (`-PaddonJars`, 454 races):

    PLAN23 P6 addon races=377 in the filtered closure=[ascension:divine_kitsune, ascension:kitsune, ascension:nine_tail_fox, ascension:six_tail_fox,
      ascension:three_tail_fox, tensurahorizons:ascendant_elf, tensurahorizons:runeforged_dwarf, tensurahorizons:tide_sovereign]

- Horizons' three races are reached (as the hard-coded list says).
- **Finding: Ascension's kitsune line (5 races) evolves out of a human-like line**, so the dynamic set takes it in; the
  plan expected "no other addon race is human-like". Two of them (`six_tail_fox`, `nine_tail_fox`) are spiritual.
  **Owner decision 2026-11-27: the kitsune line stays human-like by default** (an admin can remove it per world,
  Task 19). The probe pins the five ids.
- When: the race registry (ManasCore's Architectury registrar) is filled at mod loading; the tag is read on the
  running server during the GameTests. A `/reload` was **not** run in a test (it would reload every other test's
  data): the moment of recomputing after `/reload` is by reading, not proven (Task 17 should recompute on
  `TagsUpdatedEvent`).

### P7: Horizons' journal from a button (static only)

`JournalActionPayload(String, String, int)` exists (`new JournalActionPayload("open", "abilities:all", 0)` is the call
shape); the screen is `HorizonJournalScreen(JournalPayload)`, opened from the server's answer. **Not run on a real
client** (see "Not verified").

### P8: Vampirism's menu packet without a key (proven, one finding)

    PLAN23 P8 handler=de.teamlapen.vampirism.server.ServerPayloadHandler vampire=de.teamlapen.vampirism.inventory.VampirismMenu noFaction=unchanged

The handler is `ServerPayloadHandler.handleSimpleInputEvent` (registered in `ModPacketDispatcher`); `VAMPIRISM_MENU`
runs `FactionPlayerHandler.getCurrentFactionPlayer(p).ifPresent(fp -> fp.getTaskManager().openVampirismMenu())`,
which opens `VampirismMenu` with `player.openMenu`. Called with no key press it opens the menu for a vampire.
**Finding:** for a player in no faction it does nothing, so Task 9's "Vampirism: Menu" button must be disabled for a
player in no faction (the plan's "if P8 says the server refuses it" branch). Only the API is on our compile path, so
the proof (and Task 9) builds the payload by reflection. The same payload sent from a real client's button was not run.

### P9: `guiSkillButton` in memory (by bytecode only, not run)

`ModConfigSpec.ConfigValue.set(v)` only writes the in-memory `CommentedConfig` (`set` bytecode 26-36); writing the file
is `save()`, a separate call. So `set(false)` alone does not touch `vampirism-client.toml`, **but** any later `save()`
of Vampirism's client spec (its own settings screen, or NeoForge's config screen) would write `false` to the file.
Task 11 must restore the original value before such a save can happen, or take the widget-removal route.
`ScreenEventHandler.onInitGuiEventPost` reads the value and adds the `ImageButton` with `Init.Post.addListener`;
`onButtonClicked` (a `MouseButtonPressed.Pre` listener) reads it again. The file comparison on a real client was not run.

### P10: screen constructors and Tensura's keys (static)

- `VampirePlayerAppearanceScreen(Screen)`, `WerewolfPlayerAppearanceScreen(Screen)`, Ascension `CodexHubScreen()` (no
  argument), `SelectAmmoScreen.show()` and `SelectMinionTaskRadialScreen.show()` / `show(KeyMapping)` (static openers).
- `TensuraKeybinds`: `name` press sends `RequestNamingKeyPacket` (lambda 2, a press and a release pair); `next_mode`
  and `previous_mode` send `RequestAbilityModeChangePacket.changeModePacket(int, boolean)`; the `main_gui` and
  `reload_configs` press lambdas are empty, so their action is read elsewhere (a client tick reading the binding):
  **not resolved here**; Task 9 Step 1 must find what opens the Status screen.
- Defaults from `<clinit>`: main_gui B (66), name N (78), slots Z X C, next_mode Left Alt (342), race_ability R (82),
  dodge V (86); previous_mode and reload_configs unbound.

### P11: Alt+scroll (not run)

Only the static facts above. The class reading the scroll and the preset packet on a real client were not run.

### P12: registered race ids on the pack (proven and asserted, `-PaddonJars`)

Mysticism 186 races. The wolves: `direwolf`, `divine_wolf`, the 12 spirit wolves (`antumbra`, `gale`, `glacier`,
`magic`, `molten`, `mountain`, `ocean`, `penumbra`, `planet`, `storm`, `string`, `volcanic` + `_spirit_wolf`),
`black/blue/brown/green/purple/red/light/darkness_fang` and their `mystical_` forms, `star_wolf`, `tempest_star_wolf`,
`frost_wolf`, `nivalis_frost_wolf`, `scorch_wolf`, `blazing_scorch_wolf`; `guitar_wolf`, `amped_guitar_wolf` (left
unmapped). **Note for Task 13:** `mysticism:rimefang_drake` contains "fang": the table must list ids, never match a
substring. Ascension's Bloodfiends: `fledgling_bloodfiend`, `kindred_bloodfiend`, `blood_noble`, `elder_bloodfiend`,
`progenitor_bloodfiend`. Horizons 87 races. `theAddonRaceIdsTheTableUsesAreRegistered` asserts, when the addons are loaded, that every wolf
and Bloodfiend id above is registered, that the only other wolf-named ids are the two guitar wolves, and the counts
(Mysticism 186, Ascension 69); without the addons it logs SKIPPED.

### P13: Accessories, the compat layer and real Curios together (not run at all)

Not booted: it needs the owner's client pack. The pack does hold `accessories-neoforge-1.1.0-beta.54`,
`accessories_compat_layer-neoforge-0.1.13` and `curios-neoforge-9.6.1` (checked in the copy). Task 12 must run (a) to
(e) before it picks the renderer registration.

### P14: Enhanced Celestials and `/time set` (static)

EC 2 Core replaces the `time set` command (`TimeCommandMixin`, `TimeCommandUtil.setTime(source, time, reset)`):
`/time set <t>` moves **every level forward to time `t` of the next day** ("Set time to %s for the next day"), so the
day number goes up by one; `/time set <t> true` (reset) sets the raw day time to `t`. The lunar forecast keeps a
schedule per day (`buildSchedule`, `scheduledEventsByDay`) and `tick()` reads the event of the current day, so after a
jump the next tick shows that day's scheduled event (with EC's switch notice); the old day's event is not carried over.
This is a reading of the bytecode, not a run.

### Which proof ran in which setup

A proof that cannot run in a setup logs `SKIPPED` and passes; "absent" means its part is off, so the class is not
registered. From the matrix logs of this PR's HEAD:

| setup | P1 | P2 | P3 | P4 | P5 | P5b | P6 | P8 | P12 |
|---|---|---|---|---|---|---|---|---|---|
| def | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| nowolves | ran | ran | ran | SKIPPED | SKIPPED | SKIPPED | ran | ran | SKIPPED |
| nolinked | absent | absent | absent | absent | absent | absent | absent | absent | absent |
| off-species | ran | ran | SKIPPED | ran | ran | SKIPPED | absent | ran | absent |
| off-artifacts | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-speciesTensura | ran | ran | SKIPPED | SKIPPED | ran | ran | absent | ran | absent |
| off-supernatural | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-supernaturalTensura | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-hud | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-celestials | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-bosses | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| off-species,artifacts | ran | ran | SKIPPED | ran | ran | SKIPPED | absent | ran | absent |
| off-evolutionRites | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |
| addons (19a folder) | ran | ran | ran | SKIPPED | ran | ran | ran | ran | ran |
| addons (pack copy) | ran | ran | ran | SKIPPED | ran | ran | ran | ran | ran |
| real EC | ran | ran | ran | SKIPPED | ran | ran | ran | ran | SKIPPED |

P4 runs only with species off (by design); P12 asserts only with the addon jars.

### `NoRawKeyIdsTest` (kept)

Every id `CoveredKeyIds.all(true, true, true, true)` returns, and every key literal in `core/hud`, is one of the 58
key-binding ids of the pack's lang files (`src/test/resources/plan23/pack-key-ids.txt`), or one of the three
`tensuraslots.*` ids (Tensura Slots, removed from the pack on release, still covered for players who have it).
The physical key names in use (`key.keyboard.unknown`, `.b`, `.g`, `.h`) are listed by name. Our own ids
(`HudKeyDefaults.ALL`, the slots included) and the ids in `CoveredKeyIdsTest` (its left-alone list) are checked too;
a literal ending in `_` (an id built in a loop) must be the prefix of a known id. The list has `tensura.keybinding.name`, not `naming`.

## Not verified (needs the owner's client pack)

P7 (journal from a button), P8 from a real client's button, P9 (file before and after), P10's Status packet,
P11 (Alt+scroll on a server with the HUD mixin), P13 (a) to (e), the `/reload` moment of P6, and every "before" shot
that needs a real player state (a level-1 vampire, a Ghoul, the Controls screen): the hidden shots use the hub's demo
data, which has no such switch.

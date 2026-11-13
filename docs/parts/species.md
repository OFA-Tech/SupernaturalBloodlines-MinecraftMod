# Part: species

Requires: `vampirism`, `enchanted`, and the `VAMPIRISM_ENCHANTED` part (it reuses `FactionRiteFactory`, `RiteTargets`; `Part.requiredParts`). Hybrid, Original hybrid and tribrid also need `werewolves`. Package: `net.ofatech.supernaturalbloodlines.parts.species`. Plans: `docs/superpowers/plans/04-species.md`, `07-hunters.md`, `08-sirelines.md`, `09-werewolf-packs.md`.

Rules live in `core/bloodline` (`BloodlineRules`, `PackRules`) and `docs/superpowers/specs/SPECIES-RULES.md` is the source of truth. Checklist from the design (sections 4, 7, 8, 9). Tick items as they ship.

**Species (build order)**
- [ ] Originals and Original's blood (Rite of Immortality, white-oak rule, Original's blood item).
- [ ] Hybrid (doppelgänger blood during the transition, sire bond).
- [ ] Original hybrid (Rite of the Moon, needs the gene).
- [ ] Witch and siphoner (Rite of Ancestral Calling; siphoning).
- [ ] Heretic (a siphoner who joins the vampire faction).
- [ ] Tribrid (a witch who completes the hybrid transition).
- [ ] Eligibility: only human-like races; others are refused with a message.
- [ ] Death and respawn: species, sire and Original status persist; `siphoned_power` resets.
- [ ] Originals and white oak: non-oak lethal damage can't kill; white oak items and tree.
- [ ] Vampire traits for hybrid types (attributes by age, blood thirst, bite that poisons vampires).

**Hunters**
- [ ] Rite of the Five, `FiveRoster` (SavedData), `max_five`.
- [ ] The mark: perks from `markKills`, glow of vampires within 24 blocks at 5 kills, compulsion immunity.
- [ ] Losing the mark and choosing a potential.
- [ ] The hunter's curse (hallucinations) and when it lifts.
- [ ] The hunter instinct (Weakness after 3 days without a kill).

**Sirelines**
- [ ] `sire` recorded for turned players, converted creatures and hybrids; `SirelineIndex` (SavedData).
- [ ] `/bloodline tree [player]` and `/bloodline call`.
- [ ] Compulsion on villagers and lesser vampire mobs.
- [ ] Sire bond; optional `sireline_death` (default off).

**Werewolf bloodlines and packs (needs Werewolves)**
- [ ] Lineage BORN / BITTEN; hidden gene rolled on first login (`latent_gene_chance`); Rite of Lineage.
- [ ] Ranks OMEGA, BETA, ALPHA, TRUE_ALPHA and eye colours.
- [ ] Alpha spark: stolen, inherited, earned (True Alpha), given up (healing).
- [ ] Turning people: Alphas only; `alpha_bite_death_chance`.
- [ ] Packs: `PackIndex` (SavedData), `/pack invite|accept|leave|emissary|anchor`, pack bonus, Alpha Roar.
- [ ] Anchors for full-moon control.
- [ ] Weaknesses: mountain ash, lunar eclipse, electricity (Shocked, Taser).
- [ ] Tensura skills by rank, only in `species_tensura`.

**Debug**
- [x] `/supernaturalbloodlines debug get|set|event` (foundation).

## Shared plumbing (Task 1)

- **Hooks into the rite factory.** `vampirismenchanted.rite.FactionRiteHooks` holds a `ContextProvider` (`(target, faction, gameTime) -> BloodlineContext`, default `defaults()` + faction + time) and an `OutcomeRunner` (default `FactionRiteFactory.runFactionActions`, leave then join). `FactionRiteFactory` calls them for the preview and for the real call with the same context, and never imports a species class. `SpeciesPart.init` installs `SpeciesContext.of(p, faction, time, null)` and `OutcomeExecutor.run`; with `species` off the defaults stay (plan 3 behaviour).
- `SpeciesContext.of(p, bloodSource)` / `.of(p, faction, gameTime, bloodSource)`: faction (`FactionRiteFactory.baseFaction`), `humanLike` (`SpeciesEligibility`), Originals count (`SpeciesData`), `max_originals_per_world`, `transition_ticks`, `werewolvesLoaded`. Hunter fields (`fiveCount`, `maxFive`) are left at the defaults for plan 7.
- `SpeciesEligibility.setCheck(HumanLikeCheck)`: plan 5 installs the Tensura check (`tensura:human_like`); the default is "everyone".
- `SpeciesData` (`SavedData` `supernaturalbloodlines_species`, overworld storage): UUIDs of ORIGINAL and ORIGINAL_HYBRID players, kept by a `BloodlineAccess` listener (species changes only), so offline Originals count.
- `SpeciesConfig` (own file `supernaturalbloodlines-species.toml`, section `species`): `max_originals_per_world` (0 = unlimited), `transition_ticks` (24000). The brief put the spec inside `SpeciesData`; it is a separate class like `VampireSyncConfig`.
- **No Werewolves.** `BloodlineContext.werewolvesLoaded` (default true, set by `SpeciesContext`): when false, `BloodlineRules.apply` refuses any result that becomes a hybrid type or starts the hybrid transition with `supernaturalbloodlines.refusal.needs_werewolves`. The rules stay pure; the check is one wrapper in `apply`, so the rite preview refuses before anything changes.
- `OutcomeExecutor.run(p, outcome)`, inside `ReentrancyGuard.run(uuid, "species", ...)`: a refusal is sent as a chat message; otherwise LEAVE/JOIN as the rite factory does, then KILL_PLAYER = `hurt(source(supernaturalbloodlines:failed_transition), Float.MAX_VALUE)`. The damage type is in `#minecraft:bypasses_resistance` and `#minecraft:bypasses_invulnerability` (Resistance V and a Totem of Undying cannot save the player); if the player is still alive afterwards the executor calls `p.kill()`. Creative and spectator players are skipped. **No nested executor for the same player:** the guard channel is `species` per player, so an `OutcomeExecutor.run` that starts while another one for the same player is running (for example from a faction event the first one caused) is dropped with a warning in the log; do the follow-up after the outer call returns, or from another channel. `SpeciesData` is also re-synced from the player's species on login (`SpeciesPart.syncOriginal`).

## Hook points (verified)

Checked with `javap` on the jars in the Gradle cache: Vampirism 1.21-1.10.13, Werewolves 1.21-2.0.3.4, Enchanted 4.2.7. Earlier notes (rite lifecycle, altar data, `BloodDrinkEvent`, `PlayerFactionEvent`) are in [enchanted_tensura](enchanted_tensura.md), [vampirism_tensura](vampirism_tensura.md) and [vampirism_enchanted](vampirism_enchanted.md) and are not repeated.

### Werewolves (optional; touch only behind `ModList.isLoaded("werewolves")`)

- **Player access:** `WerewolfPlayer.get(Player)` / `getOpt(Player)` / `getOptSave(Player)` / `getOptEx(Entity)` (package `de.teamlapen.werewolves.entities.player.werewolf`). API interface `IWerewolfPlayer` (`...api.entities.player`), faction `WReference.WEREWOLF_FACTION`. `Helper.isWerewolf(Player)`, `Helper.canBecomeWerewolf(Player)`.
- **Form API:** `WerewolfPlayer.getForm()`, `switchForm(WerewolfForm)`, `setForm(WerewolfFormAction, WerewolfForm)`. `WerewolfForm` constants `NONE`, `HUMAN`, `BEAST`, `BEAST4L`, `SURVIVALIST`; `isTransformed()`, `isHumanLike()`. `FormHelper.getForm(LivingEntity)`, `FormHelper.isFormActionActive(IWerewolfPlayer)`, `FormHelper.getActiveFormAction`, `FormHelper.deactivateWerewolfActions`. A shift is a Vampirism action: the three `WerewolfFormAction`s are toggled through `ActionHandler.toggleAction(action, new ActivationContext())`.
- **Full-moon helper:** `de.teamlapen.werewolves.util.Helper.isFullMoon(Level)` is `!hasFixedTime && 12786 < dayTime % 192000 < 23216` (the night of moon phase 0; not our `MoonRule`, which is the whole phase-0 day). `Helper.isNight(Level)`.
- **Forced full-moon shift:** `WerewolfPlayer.onUpdate()`, once per second (`gameTime % 20 == 0`): if `Helper.isFullMoon`, no form action active and the player does not have the skill `ModSkills.FREE_WILL`, it toggles `lastFormAction` (else any form action the player can use) through `ActionHandler.toggleAction`. **There is a cancellable hook, no mixin needed:** `ActionHandler.toggleAction` fires `ActionEvent.ActionActivatedEvent` (a NeoForge `ICancellableEvent`, API package `de.teamlapen.vampirism.api.event`) before activating, and a cancel returns `IAction.PERM.DISALLOWED`. The forced call runs inside Vampirism's `EntityEventHandler.onPlayerUpdate(PlayerTickEvent.Post)`; a voluntary shift comes from a network packet, outside any player tick. So a listener can tell them apart with a thread-local "inside a player tick" flag (set in `PlayerTickEvent.Pre`, cleared in the last `PlayerTickEvent.Post` listener) and cancel only then. Simpler alternative: grant `FREE_WILL`, but it is a skill the player can lose or see in the tree. This is Task 4's call (hybrids shift at will); plan 9's `MoonControl` can reuse the same listener.

### Vampirism

- **Blood drink events:** `BloodDrinkEvent` (not cancellable) with `PlayerDrinkBloodEvent` (fired by `VampirePlayer.drinkBlood(int, float, boolean, IDrinkBloodContext)` through `VampirismEventFactory.fireVampirePlayerDrinkBloodEvent`) and `EntityDrinkBloodEvent`. Both only exist for vampires (`IVampire`). **Bottles:** `BloodBottleItem.finishUsingItem` only drinks when the user is an `IVampire` (`IVampire.drinkBlood(int, float, DrinkBloodContext(ItemStack))`); for anyone else it falls back to `Item.finishUsingItem`. `PureBloodItem`, `VampirismItemBloodFoodItem` and `FeedingAdapterItem` also call `drinkBlood`. **Consequence for Task 3:** a werewolf (the transition happens in the werewolf faction) fires no Vampirism blood event, so "drank doppelgänger blood / other blood" must come from our own items (`LivingEntityUseItemEvent.Finish` on `DoppelgangerBloodItem` and Original's Blood) and from whatever the spec lets a werewolf drink; do not wait for `BloodDrinkEvent`.
- **Bite:** `VampirePlayer.biteEntity(int)` / `onBite(IVampire)`, `determineBiteType(LivingEntity)` (see vampirism_tensura for the mixin that uses it).
- **Dying / DBNO (R32): the API exists.** `IVampirePlayer.isDBNO()`, `IVampirePlayer.onDeadlyHit(DamageSource): boolean` (returns true when it put the player into the state; `VampirePlayer` also has `getDbnoTimer()`, `getDbnoDuration()`, `giveUpDBNO()`). Vampirism calls it from `ModPlayerEventHandler.onLivingDeathFirst(LivingDeathEvent)` and cancels the death when it returns true. It only works for a player with Vampirism level > 0 (`VampirePlayer.getLevel() > 0`), without the `NEONATAL` effect, when `Helper.canKillVampires(source)` is false (that is true for `bypasses_invulnerability`, the `vampirism:vampire_immortal` tag and the config list `vpImmortalFromDamageSources` for living direct attackers that are not hunters, among others). The state: health is reset, forced pose SLEEPING, a timer (exact values not checked); "give up" calls `giveUpDBNO()`. So an **Original (vampire faction) already gets Vampirism's dying state** for ordinary lethal damage, and Task 2 only has to make white oak kill (`canKillVampires`/our listener at higher priority) and keep void and `/kill` lethal. An **Original hybrid is in the werewolf faction (level 0 as a vampire), so `onDeadlyHit` returns false for it**: Task 2 needs its own downed state for that species, as the ledger says.
- **Items:** `vampirism:human_heart`, `vampirism:weak_human_heart` (already in vampirism_enchanted.md).

### Enchanted 4.2.7

- **Rite JSON `entities` and `time`:** `RiteType.entities` is a `List<EntityType<?>>` of **ingredients**, not a condition on the caster or target: `GoldChalkBlockEntity.tryConsumeEntity` looks for entities of those types in the circle bounds and calls `Entity.kill()` on them (so `minecraft:player` there would kill a player). `time` is `[from, to]`, compared with `Level.getDayTime()` in `RiteType.matches` (no moon phase, and it is a day time, not a time of day modulo). `dimensions` and `weather` are the other `matches` conditions. Consequence: "the target has to be an Original" can only be a check in our own factory's `onStart` (as plan 4 Task 2 says), never in the JSON.
- **Altar range (witch x1.5):** `ServerConfig.INSTANCE.altarRange` (`ModConfigSpec.IntValue`), read at three sites in `AltarBlockEntity`: `firstTick()`, the private `setupBlockData()` (the scan of the altar's blocks) and the public `posWithinRange(BlockPos)` (does a gold chalk lie inside the altar's box; the box is `range` wide and `range + 1` along the altar's facing axis). The altar has **no owner or caster**: a witch bonus on the range has to look at a player (for example the one standing in the circle) from the chalk side, or widen `posWithinRange` for a position known to be a witch's chalk.
- **Rite power cost:** the start cost is paid in `GoldChalkBlockEntity.serverTick(Level, BlockPos, BlockState, GoldChalkBlockEntity)` after every item and entity is consumed: `PowerHelper.tryGetProvider(level, chalk.getPosHolder())` then `PowerProvider.tryConsume((double) RiteType.getPower())`. The per-tick cost is `GoldChalkBlockEntity.tryConsumePower(int)` (called by `Rite.tick()` with `tickPower`), which does the same `tryConsume` on the provider. The caster is the chalk's private `UUID caster`, set in `GoldChalkBlockEntity.use(Level, BlockPos, Player)`; `Rite.RiteParams.caster` is the same id. So the witch x0.75 mixin has two `tryConsume` call sites in `GoldChalkBlockEntity` (the `(double)` argument can be scaled with a `@ModifyArg`), plus `tryConsumePower(int)`; the siphoner paying from `siphonedPower` replaces the provider call at the same two places.
- **The altar a rite uses, and its power:** `Rite.getLevel()` / `getPos()` give the level and the chalk position; `level.getBlockEntity(pos)` is the `GoldChalkBlockEntity` (a `PowerConsumer`); `PowerHelper.tryGetProvider(level, chalk.getPosHolder())` returns the `PowerProvider` (an `AltarBlockEntity`; `PowerPosHolder.getPositions()` lists the linked altar positions). `AltarBlockEntity` has **no public getter for the stored power** (field `power` is private; capacity is on `AltarMenu`); the only public power method is `tryConsume(double)`. Read it only through a mixin accessor or reflection, or avoid needing it.

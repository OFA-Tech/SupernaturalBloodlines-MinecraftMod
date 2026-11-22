# Known limits

Things that **don't work the way you may expect**, or are **not finished or not checked in a real game yet**. The mod is at version **0.2.0-alpha**. Everything is covered by automated game tests on a test server; the "not checked" items need a person in a real client.

## Not possible

- **Switching the species part off and on again keeps every rite known.** With the `species` part off, every learnable rite counts as known (nothing could teach them), and turning the part back on does not take that away: knowledge is only ever added. `/bloodline rites forget <player> all` puts a player back to what their kind, their lore and their performed rites give.

- **The rites are sealed in the book, not hidden from the files.** Every rite's name and text is in the mod's client files (the same jar every client has), so a player who unpacks it can read the rite entries and circles. The lock is for play, not secrecy. Lore items (grimoires, Torn Pages) can be handed from one player to another, and JEI lists Enchanted's rites as Enchanted does; both are left alone.

- **Born werewolves can't unlock the four-legged form 5 levels sooner (R90).** Werewolves has no unlock for that form that a player can reach, so there is nothing to move. The "beast" form covers both beasts. A born wolf still gets half the forced full-moon shifts.
- **Enchanted garlic does not hurt vampires.** It only counts as garlic in recipes and the cauldron colour. The garlic crop and the diffusers use Vampirism's own block, not the tag.
- **A Werewolves bite does not burn on vervain, and is not a source of blood thirst** or "other blood" in the hybrid transition: Werewolves gives no hook for it.
- **Tensura skills on a sired player:** Starved and Usurper don't check for subordinates, so a sire can drain or rob a sired player.
- **Mastery of a layer skill is lost** when the species goes and comes back (the skill is forgotten and learned fresh). Same for rank skills.

## Supernatural ladders

- **A rite counts when it finishes at once.** Enchanted fires no event when a rite is done, so the mod watches the moment a rite's start finishes the rite without cancelling it. Every rite the ladders ask for (Ward of Return, the Rite of Ancestral Calling, the blood rites, the cure, the curses of ours) works that way. A rite that keeps running over time and ends later is not counted; no ladder task asks for one. If an Enchanted update changes the hook, the log says "RiteCompleteMixin hook MISSING" and rites stop counting until the mod is updated.
- **A task whose part is switched off is not shown or asked for.** If you turn a part off (for example `species`), the tasks it fires (compelling, siphoning, the rites of that part) are dropped from the ladders, and a rung with nothing left is reached on its own.

## Rites, rollbacks and configs

- **Rolling back forgets severances.** An older build doesn't know them, so severed bonds come back. Reset apex-race players first (see [For server owners](For-Server-Owners#removing-the-mod-or-tensura)).
- **The apex pick is refused silently on a config mismatch.** If `apex_evolutions_enabled` is on for the client and off for the server, the client shows the apex race and the server refuses the pick with nothing logged. Keep the config equal.
- **Severance has no consent check**, and a new sire replaces the old one (the tree follows the new sire, and the line is whatever the new sire's is).
- **Taglock rites need the owner online or loaded.** The Rite of Doppelgänger Blood and the Rite of Revelation read the owner's genes from the live player or villager, so a taglock of an offline player or of an unloaded villager is refused ("The taglock's owner must be nearby or online.") and its ingredients come back. A player's genes are stored in their save, which is not read while they are away.
- **The Vampire Compass forgets its target when it is not held** (inventory, dropped, in a container you closed). A compass put in an item frame or a hopper line is not emptied, and one saved in a chest by an older version keeps its last target until a player carries it.
- **A refused Rite of Doppelgänger Blood tells the caster that the taglock's owner is not a doppelgänger** ("This taglock does not belong to a doppelgänger."), so anyone with a player's taglock can find out, at the cost of one rite, whether that player carries the gene (the owner learns nothing). That is how the rite works as a test; set `doppelganger_rite_taglock = human` to remove the distinction.
- **The doppelgänger gene can only be set by command for an online player.**
- **Sire's Blood:** anyone can be hit for blood; only the rite checks it was the right sire. The Silver Dagger can't be crafted with no silver mod (neither Werewolves nor Tensura). Art for both items is a placeholder.

## Not done yet

- **Eyes are particles only.** Werewolves in a wolf form show two glowing dust particles at eye height. The render layer on the model is not done.
- **The Taser** has placeholder art and spends a charge on **every** hit, not only on supernaturals.
- **No race icon** for the nine apex races in Tensura's evolution screen, and the evolution tree may not draw the link back (`getPreviousEvolutions` is empty). Names, stats, EP and signature skills are **placeholders**.
- **Placeholder textures** for almost everything this mod adds (Blood Stone, Condensed Magicule, poppets, rings, wood, vervain, wolfsbane, lichen, Taser, effect icons).
- **No grey screen tint** for a desiccated player. A desiccated vampire can still *start* a Vampirism bite: it drinks nothing, but the victim still takes the bite.
- **Hunter outline:** the colour is the entity's team colour (white without a team). A glow that starts while an entity is outlined is lost when the outline ends. No Vampirism hunter skin on the phantom zombie. The outline and the hallucinations were loaded, not seen, in a client.
- **Hunter kill credit:** a vampire that dies of the dying state's timer after more than 10 minutes, or whose killer logged out, gives no kill.
- **Mountain ash:** the wall is collision only (no path planning, so mobs may push against it), explosions and pistons destroy it, and it has no flammability, leaf colour or potted sapling.
- **Sun Ring** does not stop Weakness in the sun. **Rings cannot be rebound.**
- **Wolfsbane and lichen:** the lichen does not spread, the wild crops do not generate (seeds come only from chests), the common strain has no ash or potion, and a coated weapon has no tooltip.
- **Shocked:** natural regeneration is recognised by reading the call stack for the food bar, so a mod that heals through the food bar is blocked too. A **vampire's own blood regeneration is not blocked.**
- **Sire bond gaps:** damage from an area cloud with no owner (a dispenser), or from an arrow whose owner is offline or unloaded, still gets through.
- **Sire calls:** a creature that can't path (a villager in a hole) stays put. `/bloodline call` uses a plain walk for everything: Tensura's follow is not used. An offline sire's `ISubordinate` creature is not tamed later.
- **Hybrid transition timer:** the client's copy of the time left is stale until the next real change or login.
- **Enchanted 4.2.7 curse quirks** (not ours): a first application of a curse always has strength 0, and a curse applied in the same tick as its removal is lost.
- **Hybrids and the eclipse:** while an eclipse runs, hybrids can't shift either.

- **The turning is fed from a bottle for non-vampires.** Only a vampire can bite in Vampirism, so before you are a Ghoul the only way to drink human blood is a Vampirism blood bottle (right-click), and a bottle counts as human blood whatever it was filled from. The turning's clock is the player's online time; the client's copy of it is stale until the next real change or login.

- **The turning, a refused join:** if Vampirism or another mod refuses the join into the vampires at the moment a player drinks, they are told and the turning is over (the bottle is used up). They have to start again. The failed-turning kill and a Ghoul's death were tested on test players only, not with a real client's Vampirism "dying" screen.
- **Switching Tensura parts off with Ghouls around:** with `species_tensura` off a Ghoul keeps a plain human (or other) race and Tensura evolutions are **not** blocked (the level and altar gates are Vampirism-side and still hold); a player who already has the `tensura:ghoul` race keeps it until `vampirism_tensura`'s cure mapping runs. With `vampirism_tensura` off, `species_tensura` still sets a cured Ghoul back to `tensura:human` itself.
- **A hunter or a werewolf cannot evolve along Tensura's vampire line** (the evolution would pull them out of their faction): chat says why and nothing changes. A player with **no faction** (a Tensura-only player) can, and is joined to the vampire faction by it.
- **Tensura's evolution screen for a Ghoul:** Tensura's own code answers "evolved" and plays its sound even when the evolution was refused, and its progress bar can show 100%. The race does not change and chat says why. Only evolutions are refused: an admin's race command still works on a Ghoul.
- **Ghouls and Vampirism skill points:** Vampirism's skill-point providers can only add points, so a Ghoul is held at level 1 by the level cap, not by a lowered point count. A command that sets a Ghoul's level above 1 is refused too (the same event).

## Design points the owner may still change

- Farming True Alpha **defender kills** at a spawner beside a villager.
- With the **artifacts part off**, a bitten non-Alpha wolf has no way to shift at will.
- **Undead treatment** is wider than holy damage: it affects a plain Vampirism vampire too.
- **Apex races** give more max health than the divine tier, and their values are final (see `docs/balance.md`); only the textures and icons are still placeholders.
- **Sire link** makes sired players unable to hurt their sire, and siblings allies. Siring names the sired automatically and ignores `playerNaming`; with the sire offline or short of magicule the naming waits for the next login of either.

## Not checked in a real game yet

Automated tests cover the rules, but these still need a person with a real client (two clients where it says so):

- Every rite at a real chalk circle and altar (item matching, entity sacrifice), the Rite of Severance with its real ingredients, and the Sire's Blood tooltip and models. Poppets at a real spinning wheel.
- **A real Tensura soul attack against a bound Spirit Poppet** (a release blocker; the tests only use hand-dealt soul damage).
- **The Resurrection Ring in an Accessories slot** (the OFA pack): the charge may be taken from a copy, in which case the ring would keep reviving. Die to a supernatural three times and see the ring empty. **Do not rely on it before this is checked.** The three rings through Accessories' Curios layer, in general, are unverified.
- The two-client playthrough: Original, Original's Blood, a werewolf friend turned hybrid, Original hybrid, witch, tribrid.
- A pack of 1 Alpha and 3 Betas: bonus and Regeneration, the roar (buff, submit, eye glow seen by the other client), the eyes.
- The eclipse sky: `/supernaturalbloodlines debug eclipse start` with a real werewolf.
- The forced shift on a real full moon; the downed screen; rite entity consumption.
- **Hunter outline** visible only to the hunter; the hallucinations; kill credit through the dying state with a real vampire.
- `/bloodline call` and compulsion with two clients (the chat buttons, the trade screen not opening, a walk to the caller).
- A real Sun Ring in sunlight; a real full-moon shift with the Moon Ring; the compass needle; the mountain ash and wolfsbane visuals.
- **Tensura:** an elf (or human) taken to the apex offer in the real evolution screen; a sire's automatic naming of a sired player at a real login; an Alpha's rank skills in the skill list; the analysis screen for Vampirism, Werewolves and Enchanted mobs (EP numbers are first guesses); the evolution and skill screens of a layered player.

If you find something that behaves differently from this wiki, please [open an issue](https://github.com/OFA-Tech/SupernaturalBloodlines-MinecraftMod/issues). See [Troubleshooting](Troubleshooting).

- **Tensura Slots is no longer needed.** The ability HUD's right column (the next preset) works without it, through one small mixin (switch `next_preset_active`). With Tensura Slots still installed both work, but its own second column keeps drawing next to ours and its keys are unbound by the HUD, so remove it. Tensura Slots' own hook also accepts the next preset by itself: with it installed, `next_preset_active=false` no longer refuses right-column skills, and a skill used in the wrong mode is still accepted (checked with the real Slots jar, `-PtensuraSlotsJar`). The hold, press and Alt checks against Tensura's own keys were done from the code and the tests, not yet in a running client.

- **Ability HUD keys:**
  - **Alt + scroll is Tensura's own**: it is bound to Tensura's `next_mode` key (Left Alt) and moves through the nine presets; the HUD adds nothing to it. In Supernatural mode Alt + scroll still moves Tensura's presets.
  - **Shift + R and Shift + F belong to Book of Dragons in the OFA pack**, so sneaking blocks slots 1 and 2 there.
  - A slot key also fires with Ctrl or Shift held (Alt must work for Tensura's mode change).
  - **Creative and spectator mode show no Vampirism blood bar.** Vampirism itself does not draw its blood bar when the game mode has no hunger (`hasExperience()` is false), so with `blood_in_food_spot = false` a vampire in creative sees no blood value at all (in survival and adventure Vampirism's bar is back above the hotbar). With the setting on, the blood in Tensura's food spot shows in every game mode.

## Enhanced Celestials 2 (optional)

- **Not checked in a real game.** Tested only with a stand-in for Enhanced Celestials and against its jars (Core 2.0.3.4, Default Lunar Events 2.0.1.4, checked with javap). A first two-client run, with and without it, is still to do. Enhanced Celestials is young (about three months): if an update changes its API, the part logs one warning and the lunar events are ignored; nothing else breaks.
- **An event is not a full moon.** A Blood, Blue or Harvest Moon can fall on any phase. Rites that need the full moon, the Werewolves forced shift and werewolf moon control still follow the real moon.
- **Four jars.** Enhanced Celestials 2 Core has no events of its own: you also need Default Lunar Events (for Blood, Blue, Harvest and super moons), CorgiLib and Data Anchor. With the core alone only our lunar eclipse shows up.
- **NPC werewolf packs and the full-moon bosses do not exist yet**, so the Blood Moon does not change their chance and a super moon does not change their weight today. Those settings come with them.
- **Removing the mod in the middle of a lunar eclipse** (with Enhanced Celestials installed) makes Enhanced Celestials log an error about our event and forget its saved forecast (the next lunar events are rolled again). End the eclipse first: `/supernaturalbloodlines debug eclipse stop`.
- **The "Tonight: Blood Moon" line on Home and a Guide paragraph are not built.** Only the almanac on the Path page shows the events.
- **The eclipse light.** The red light of our eclipse as an Enhanced Celestials event is a guess (`c0403a`); it may need a different colour once someone has seen it.

## Packs (plan 14)

- **The party link has not been seen with the real Open Parties and Claims running.** It is written against the 0.33.0 jar (and the Team Claims 1.5.0 fork, which has the same classes) with javap, reached by reflection, and tested with a stand-in. Five failed calls in a row log one warning and turn it off for the rest of the session; a single failure changes nothing.
- **The Alpha Trial cannot happen yet.** `AlphaTrial.win` is the hook plan 15 will call: until then nothing in the game makes a Beta a second Alpha except an invitation to an Alpha.
- **Do not downgrade this mod on a world that has packs, and back the world up before you update.** The pack file (`supernaturalbloodlines_packs`) gets new lists (strengths, alliances, recorded parties, notices, sparks, last-seen times); an older version drops them: **stacked sparks go back to one**, and the packs' parties are forgotten, **so anyone the pack put in a party stays in it**.
- **A player already in another party** (any Open Parties and Claims party) is not added to the pack's party; they stay in the pack and are tried again every 10 seconds.
- **A cured second Alpha, and a cured Beta, stay in the pack as Packmates.** Leave with `/pack leave`.
- **Open Parties and Claims cannot hand a party to someone else.** The party's owner stays the player who founded it, whoever leads the pack now: when leadership moves, the pack makes a new party for the new leader, takes its own members out of the old one and leaves the old owner's party (and the old owner in it) as it is.
- **The party is checked every 10 seconds** (and at once when somebody joins, leaves or is kicked): leaving the party leaves the pack within that time.
- **The strongest Alpha is only the successor.** It is worked out when the leader leaves, loses the spark or gives it up, from the last level the game saw each Alpha with (an offline Alpha counts with the level they last had). An ordinary death of the leader changes nothing. A joining or levelling Alpha never takes the lead; only a duel does.
- **A leadership duel needs both of them online, and a duelist who loses their connection has `duel_reconnect_seconds` (60) to come back, once per duel.** Each has a window of their own and the duel is paused (its 2 minute clock stops) while either is away: the other leaving too wins nothing. Somebody still away when their own window ends forfeits; if both windows end with both still away nobody wins and the pair is barred for 10 minutes. A second disconnect of the same duelist is a forfeit. A server that is stopping settles nothing, and the duel is not kept across a restart.
- **Packmates have no rank of their own.** They keep their race and ranks; a Packmate who is later turned into a werewolf by another Alpha's bite changes pack with the bite.
- **Upgrading with Open Parties and Claims:** packs that exist already get a party on the first run. Set opac_link_enabled=false before upgrading to opt out; turning it off later does not remove the parties.
- **A leadership duel needs the two within 64 blocks and in one dimension**: asked or accepted from farther away is refused, and walking out of range (or into another dimension) while it is on ends it **and bars the pair for 10 minutes**. A duelist cannot be removed from the pack in the middle of it: `/pack kick` refuses, and leaving the pack's party only takes effect once it is over.
- **The absent leader is measured by the wall clock** (the time the leader was last seen: at logout, and renewed every hour for every pack member who is online), not by game days. **The time a server was off does not count, and a restart does not erase an absence**: the world keeps the last time it knew the server was running (written at most once a minute and at a clean stop), and at the start every recorded time moves forward by exactly the gap. A crash costs a leader at most the age of the last save of the world (the time the server was running is saved with it, so the gap after the crash is forgiven only up to the last save); a world from before this was recorded is not shifted, and a leader with no time on record is not deposed (the time starts the first time the check sees them away).
- **Stacked sparks are not shown for a wolf that was offline since before they were stacked** until they log in once (the index learns the count when it changes online).
- **An ordinary death of the pack leader changes nothing**, and so does a kill inside the pack outside a duel: only a duel, a leader who stays away, or a lost spark moves the lead.
- **A restart clears the duel waits and the grace.** The 10 and 5 minute duel cooldowns, the guard after a defeat and the `left_pack_grace_minutes` memory are kept in memory only.
- **A leader who is away with no second Alpha keeps the lead.** There is nobody to hand the pack to, and a second Alpha who is away too is not a successor either (so two absent Alphas never pass the lead back and forth).
- **A defeat by lava moves the loser out of it only to a spot they can see from where they stand** (a ray from their eyes that hits no block: never through a wall) and only when the server sees them standing in lava; the 5 seconds of Fire Resistance are given in every case.
- **A member the leader removes from the pack's Open Parties and Claims party is treated as having left it by choice**: Open Parties and Claims does not say who removed them, so they leave the pack and get the same kill grace as `/pack leave`. (Only the owner, who is the leader, can remove people from the party, so it is the leader's own doing; a kick with `/pack kick` is the way to remove someone without the grace.)

- **Line rites have no consent check:** a witch can free an unwilling player, as with Severance.
- **Original lines:** a rite target must be online and in the same world as the circle. Both line rites need a lunar eclipse, and the eclipse comes from Werewolves: without it the rites cannot be performed. NPC Originals arrive with the boss update: until then there is no NPC Original to kill or desiccate, so line death and the line warnings only happen for player Originals (the NPC roots only group creatures). A world from before lines gets its line index rebuilt at login (a player) or when the creature first loads.

## Wild packs (plan 15b)
- A pack spawns once per player per night; there is no way to ask for another or to cancel it.
- A weaker spark grows with online time, not active time: someone away from the keyboard grows it.
- Wolf Spirits are vanilla wolves tamed to the caster, not the Werewolves mod's own spirit wolf.
- Brothers and chests: whether a Brother may use a chest is decided by offering the owner's right click on it to the game, so other mods may see that click (a "you cannot open this" message to the owner, a log line). It is only made while the owner is in the same dimension and within 64 blocks of the Brother's post; otherwise the Brother hunts but stores and sorts nothing. A Brother whose ledger entry was lost (a damaged save) is freed when it next loads.
- Brothers: a hunting trip is one action, not a walk (the Brother does not travel to the animals and back), and it ignores Open Parties and Claims, so it stays within 48 blocks of its post. A Brother that is far from every player is not loaded and cannot be ordered until you go near it (its row stays on the Pack page).
- The NPC profile is a panel under the Pack page's wolf rows, not a page of its own; sire, line, genes and anchor show "?" for a wolf.
- A Tensura subordinate is listed on the Pack page but takes its orders in Tensura; only the subordinates of a pack member or of a Brother join, one step down.

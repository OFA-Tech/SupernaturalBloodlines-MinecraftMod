# Known limits

Things that **don't work the way you may expect**, or are **not finished or not checked in a real game yet**. The mod is at version **0.1.0**. Everything is covered by automated game tests on a test server; the "not checked" items need a person in a real client.

## Not possible

- **Born werewolves can't unlock the four-legged form 5 levels sooner (R90).** Werewolves has no unlock for that form that a player can reach, so there is nothing to move. The "beast" form covers both beasts. A born wolf still gets half the forced full-moon shifts.
- **Enchanted garlic does not hurt vampires.** It only counts as garlic in recipes and the cauldron colour. The garlic crop and the diffusers use Vampirism's own block, not the tag.
- **A Werewolves bite does not burn on vervain, and is not a source of blood thirst** or "other blood" in the hybrid transition: Werewolves gives no hook for it.
- **Tensura skills on a sired player:** Starved and Usurper don't check for subordinates, so a sire can drain or rob a sired player.
- **Mastery of a layer skill is lost** when the species goes and comes back (the skill is forgotten and learned fresh). Same for rank skills.

## Rites, rollbacks and configs

- **Rolling back forgets severances.** An older build doesn't know them, so severed bonds come back. Reset apex-race players first (see [For server owners](For-Server-Owners#removing-the-mod-or-tensura)).
- **The apex pick is refused silently on a config mismatch.** If `apex_evolutions_enabled` is on for the client and off for the server, the client shows the apex race and the server refuses the pick with nothing logged. Keep the config equal.
- **Severance has no consent check**, and a new sire replaces the old one (tree and sireline death follow the new sire).
- **Sire's Blood:** anyone can be hit for blood; only the rite checks it was the right sire. The Silver Dagger can't be crafted with no silver mod (neither Werewolves nor Tensura). Art for both items is a placeholder.

## Not done yet

- **Eyes are particles only.** Werewolves in a wolf form show two glowing dust particles at eye height. The render layer on the model is not done.
- **The Taser** has placeholder art and spends a charge on **every** hit, not only on supernaturals.
- **No race icon** for the seven apex races in Tensura's evolution screen, and the evolution tree may not draw the link back (`getPreviousEvolutions` is empty). Names, stats, EP and signature skills are **placeholders**.
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

## Design points the owner may still change

- Farming True Alpha **defender kills** at a spawner beside a villager.
- With the **artifacts part off**, a bitten non-Alpha wolf has no way to shift at will.
- **Undead treatment** is wider than holy damage: it affects a plain Vampirism vampire too.
- **Apex races** give more max health than the divine tier, and all their values are placeholders.
- **Sire link** makes sired players unable to hurt their sire, and siblings allies. Sire naming bypasses `playerNaming`.

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
- **Tensura:** an elf (or human) taken to the apex offer in the real evolution screen; a sire naming a sired player through the real menu; an Alpha's rank skills in the skill list; the analysis screen for Vampirism, Werewolves and Enchanted mobs (EP numbers are first guesses); the evolution and skill screens of a layered player.

If you find something that behaves differently from this wiki, please [open an issue](https://github.com/OFA-Tech/SupernaturalBloodlines-MinecraftMod/issues). See [Troubleshooting](Troubleshooting).

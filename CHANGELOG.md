# Changelog

## Unreleased: artifacts

- Sun Ring (craft: 4 gold ingots around glowstone; rite *Ward of Daylight*: ring, 2 sunflowers, 4 glowstone dust, medium circle, 6000 power): bound to the vampire on the circle (data component `supernaturalbloodlines:bound_to`). While they wear it (Curios `ring` slot or inventory) Vampirism's sun damage is cancelled, and a hidden sunscreen effect also stops the sun kill and nausea (Vampirism has no sun hook). Weakness in the sun is not stopped. A ring bound to someone else, or unbound, does nothing.
- Moon Ring (needs Werewolves; craft: 4 silver ingots around an amethyst shard; rite *Ward of the Moon*: ring, 2 amethyst shards, 2 silver ingots, medium circle, 6000 power; bound to a werewolf): moon control (R96). The forced full-moon shift is skipped for the owner. Half forms at will, full forms from Werewolves level 8 (`werewolves.full_form_level` in the common config).
- Shared `MoonControl` and `ForcedShift` (package `shared.werewolves`): the hybrids' forced-shift cancelling moved there; hybrids are one control source, the Moon Ring another. No new mixin.
- A rite factory `supernaturalbloodlines:bind_item` (input, output, optional faction): binds the person on the circle to the output item; wrong faction, a missing input or an already bound input refunds the ingredients and says why.
- Curios is optional: the rings are in the tag `curios:ring`; without Curios they work from the inventory.
- Placeholder textures for both rings.
- Vampire Compass (rite *Rite of the Vampire Compass*: compass + vervain, medium circle, 2000 power): the needle points at the nearest vampire, hybrid or Original within 64 blocks (not yourself), refreshed once a second on the server through the vanilla lodestone tracker; with nobody in range it spins. Client: an `angle` item property with 32 placeholder needle frames.
- Hearing Device (rite *Rite of the Hearing Device*: note block, 2 iron ingots, amethyst shard, medium circle, 3000 power): right click stuns every supernatural (vampire, werewolf, species; mob or player) within 5 blocks for 5 seconds (Slowness V + Nausea), 60 second cooldown. The user, humans and other mobs are not affected.
- Mountain ash: sapling, log, leaves and planks (the tree grows from the sapling and generates rarely in forest biomes through a biome modifier), and *mountain ash dust* (stonecutter: 1 log -> 4 dust). Dust is placed like redstone dust and lays a line that gives no power. Vampires, werewolves, hybrids, Originals, heretics and tribrids cannot walk across it (a 1.5 block wall for them only; witches, siphoners and humans pass), mobs of that kind path around it, projectiles they fire are discarded on contact, and they cannot break it by hand. A line flagged as laid by a pack Emissary (the flag exists, plan 9 sets it) also stops the mobs in `#supernaturalbloodlines:supernatural_mobs` (only the Vampirism ghost for now).
- Test template `tall` (16x10x16) for tests whose entities are taller than a block.

## Unreleased: species

- Originals: the Rite of Immortality makes one (needs a full moon, a white oak sapling, a golden apple, a human heart and a villager). +4 max health, +20% attack, and lethal damage that is not white oak, the void or `/kill` cannot kill them (vampires use Vampirism's dying state, Original hybrids a downed state of 30 seconds with a Give up / Wait screen).
- White oak: log, planks, sapling (grows a white oak tree) and the White Oak Stake, the only weapon that kills an Original.
- Original's Blood: Rite of Original's Blood fills a glass bottle (Originals only); drinking it gives the `original_blood` effect, and a werewolf who dies with it in them starts the hybrid transition. The last blood drunk before death is the sire.
- Hybrid transition (24000 online ticks, config): Doppelgänger Blood completes it, any other blood makes it unstable and then kills, running out of time kills. The timer survives a relog and a respawn.
- Hybrid traits: movement speed and attack damage that grow over 50 in-game days (1,200,000 ticks), blood thirst (Weakness at 0, Strength from 15), hybrid shifts at will (the forced full-moon shift is cancelled), and the sire bond (a hybrid cannot hurt their sire, directly or by projectile, cloud or pet).
- Venom and Hybrid's Blood: a werewolf's melee hit on a vampire poisons them for 10 minutes (1 damage pulses, hallucinations, death at the end; Originals are weakened instead); Hybrid's Blood (a rite, one glass bottle) cures it.
- Original hybrid: the Rite of the Moon turns an Original with the werewolf gene into one. Original hybrids cannot be cured.
- The R24 gene kill: a vampire or Original with the hidden gene who kills a villager or a player becomes a bound hybrid; the faction level carries over (capped) when the base faction changes.
- Witches and siphoners: the Rite of Ancestral Calling (25% siphoner by default). Witches widen an altar's range x1.5 and pay x0.75 for rites (mixins into Enchanted, each with `require = 0`, a missing one only turns the bonus off). Siphoners drain altar-power blocks and creatures for siphoned power (cap 1000, reset on death) and can pay a rite's start cost with it.
- Heretics (a siphoner who joins the vampires) keep siphoning and the witch bonus. Tribrids (a witch who completes the hybrid transition) are hybrids with the witch bonus and siphoning. A siphoner cannot start the hybrid transition.
- Hybrid-type species and their rites need Werewolves; without it they are refused and the rites are not loaded.
- Fixed flaky GameTests: the test template was 1x1x1, so cells next to a chunk edge had entities in chunks that do not tick. It is 16x1x16 now.
- Known pending (manual): the two-client playthrough (Original, Original's Blood, a werewolf friend turned hybrid, Original hybrid, witch, tribrid), the forced-shift check on a real full moon, the downed screen, placeholder textures. Moonstone has no loot source yet. The Werewolves bite as a source of thirst and venom is not hooked (no hook exists).

## Unreleased: vampirism_enchanted

- Plant parity (data): Enchanted garlic is in the `c:crops/garlic` tag, so all Vampirism recipes that use garlic (and the cauldron colour) accept it. **Limitation:** the tag is only read by recipes. The garlic crop's effect on vampires and the garlic diffusers do not read it, so an `enchanted:garlic` crop does not hurt a vampire.
- Enchanted wolfsbane converts to and from Werewolves' wolfsbane (two shapeless recipes; Werewolves has no tag): 1 Enchanted flower gives 1 Werewolves wolfsbane, 2 Werewolves wolfsbane give 2 Enchanted flowers (a single wolfsbane is Werewolves' own purple dye recipe, so a 1:1 recipe would collide with it). Only with Werewolves.
- Altar power: Vampirism garlic and vampire orchid, Werewolves wolfsbane and wolf berry bush (8 per block, up to 20).
- Blood data: `entity_blood` entries for the Enchanted familiar cat (3) and mandrake (0).
- A rite factory `supernaturalbloodlines:faction_rite` (event, faction requirement, optional full moon) that applies a bloodline event to the person on the circle through `BloodlineRules`; a failure refunds the ingredients and says why.
- Rite of Cleansing Blood (garlic, holy water, golden apple): cures vampirism. Rite of the Silver Moon (2 silver ingots, wolfsbane, full moon; Werewolves only): cures lycanthropy. Originals and hybrids follow the bloodline rules.
- Rite of Blood Offering: 4 full Vampirism blood bottles and a stone make a `blood_stone` block (altar power 25, up to 16). Empty bottles are not accepted.
- Sun Poppet: protects its bound player from a lethal hit of Vampirism's sun damage. Silver Poppet (Werewolves only, not registered without it): protects from a lethal hit by a silver item (`werewolves:type/silver`), a silver bolt or other silver ammunition shot at the player, or a werewolf bite. Both follow Enchanted's rule (poppets are only consulted on a lethal hit) and are made at the spinning wheel from an Enchanted poppet, anointing paste and glowstone dust or a silver ingot.
- Known pending: manual in-game checks of the rites at a real chalk circle, the poppets at a real spinning wheel, and the placeholder textures (block and poppets).

## Unreleased: enchanted_tensura

- Altar power (data): Tensura magic ore, magic blocks, crystal blocks, magisteel and magic metal blocks power Enchanted altars (6 per ore up to 20, 10 per block up to 10).
- EP data (`entity_existence`) for the Enchanted mandrake and familiar cat. Numbers are first guesses.
- Two curses: *Magicule Drain* (slowly lowers magicules, never below 0) and *Skill Seal* (one random learned skill can't be activated or toggled on). Both are lifted by Enchanted's remove-curse rite and cleared on death.
- Two rites: *Purification* (holy damage to Tensura-undead races and undead mobs in 8 blocks; living things are spared) and *Magicule Offering* (the caster's magicules become a `condensed_magicule` block; fails with a message and takes nothing if the caster has too few).
- New block `condensed_magicule` (altar power 30, up to 16), new damage type `holy_purification` (counts as holy for Tensura).
- Spirit Poppet: protects the player it is bound to from lethal damage of the `tensura:is_spiritual` types (soul scatter, soul consumed, synthesise), following Enchanted's rule (poppets are only consulted on a lethal hit). Spinning wheel recipe from the Enchanted poppet, a medium magic crystal and soul sand.
- Known pending: manual in-game checks of the rites at a real chalk circle, the curses on a cursed player, the Tensura analysis screen for Enchanted mobs, and the placeholder textures (block and poppet).
## Unreleased: vampirism_tensura

- Unified vampire sync: a Tensura race in the vampire line joins the Vampirism vampire faction at a level floor (`vampire` 1, `vampire_overcomer` and `vampire_lord` 7, `divine_vampire` max; `ghoul` no faction change). The floor never lowers an existing level. Joining Vampirism keeps the Tensura race, except an exact `tensura:human` becomes `tensura:vampire` automatically (R20a); a cure resets the race to `cure_race` (default `tensura:human`) only if it is in the vampire line.
- Single sun burn: Tensura's own vampire burn and sun debuffs are disabled for faction vampires (2 mixins, see `docs/mixins.md`); Vampirism's sun handling wins.
- Hunter and werewolf faction rules by Tensura race tags: `tensura:undead` and `daemon` can't be hunters (and leave the faction if they become one); `undead`, `daemon` and `no_blood` can't be werewolves.
- Tensura holy damage hurts Vampirism vampires x1.5 (`tensura_holy_vs_vampire`). Vampirism holy water already counts as Tensura holy damage.
- Bloodless races can't be bitten: a bite mixin for `tensura:no_blood` race players and an `entity_blood` data map (blood 0) for Tensura bloodless mobs.
- Werewolf skill grants: werewolf levels grant configured Tensura skills (default `tensura:godwolf_sense` at max level).
- EP data (`entity_existence`) for Vampirism and Werewolves mobs. Numbers are first guesses, to tune after playtesting.
- New config file `supernaturalbloodlines-vampirism_tensura.toml`.
- Known pending: manual check in game that the Tensura analysis screen shows the EP of Vampirism and Werewolves mobs (covered only by registry GameTests).

## Unreleased

- Project created from the NeoForge 1.21.1 ModDevGradle template.

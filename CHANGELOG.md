# Changelog

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

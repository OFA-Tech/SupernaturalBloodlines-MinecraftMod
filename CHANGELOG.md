# Changelog

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

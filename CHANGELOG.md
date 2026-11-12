# Changelog

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

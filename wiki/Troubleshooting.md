# Troubleshooting

See also [Known limits](Known-Limits) for things that are not finished.

## The game won't start

- **A version error naming a linked mod:** the mod declares each linked mod with a **version range** (see [Installing](Installing#requirements)). A Vampirism, Werewolves, Enchanted, Tensura, ManasCore or Curios outside the range stops the game. Use the tested versions.
- **A part says it failed to initialise** (`Part X failed to initialise, skipping it`, with an error): that part is skipped and the rest keeps running. Usually a linked mod was updated and a class changed. Report it.

## A part is not working

Read the log line for it at startup:

| Log line | Meaning |
|---|---|
| `Part X: enabled` | on |
| `Part X: disabled (missing: tensura)` | a mod it needs is not installed |
| `Part X: disabled (config)` | switched off in `[parts]` |
| `Part X: disabled (requires [VAMPIRISM_ENCHANTED])` | a part it builds on is off |

Also search the log for `hooks in place`, `MISSING` and `hook is not in place`: they say whether the Tensura mixins found their target. If a hook is missing, the feature that uses it is off and the rest still works.

## I can't join the server

A **registry mismatch**: the client and the server don't have the same mods, the same version of this mod, or the same part switches. Fix the client to match. See [For server owners](For-Server-Owners#common-configs-must-match).

## A player can't load after the mod was removed

The player was on an **apex race**. ManasCore can't load a saved race that is not registered. Put the mod back, reset the race with Tensura's race command, then remove it. See [For server owners](For-Server-Owners#removing-the-mod-or-tensura).

## A rite does nothing

- The **chalk circle** must be the right size (small or medium) and made of the right chalk (ritual chalk, nether chalk for the curses). The ingredients must be **on the centre** (items as items, a sacrifice as a living entity **inside** the circle).
- The altar must have **enough power**. A siphoner with enough siphoned power doesn't need one.
- The rite may have **refused**: the ingredients come back and the caster sees a message on the action bar. The common ones:

| Message | What to do |
|---|---|
| "This rite can only be performed on a full-moon night." | wait for the night of the full moon, after dusk (not in the Nether or the End) |
| "The one in the circle does not belong to the right faction for this rite." | the target must stand on the centre block and be in the faction the rite names |
| "The rite would change nothing." | the target already has what the rite gives |
| "The rite cannot take hold: the new faction would not accept them." | Vampirism or Werewolves refused the join (for example an undead Tensura race) |
| "Your race cannot take a bloodline." | the Tensura race isn't human-like |
| "You already belong to a faction." / "You already carry a bloodline." | the target has a faction or a species |
| "The world already has as many Originals as it allows." | `max_originals_per_world` |
| "The Five are already complete." | `max_five` |
| "An Original cannot be cured this way." | only The Cure works on an Original |
| "You do not carry the werewolf gene." | the target needs the gene (Rite of Lineage) |
| "This bloodline needs the Werewolves mod." | install Werewolves |
| "The rite needs a desiccated Original in the circle." | hit an Original with the Ash Dagger first, within 6 blocks |
| "You do not have enough magicules to make this offering." | the Rite of Magicule Offering needs 5000 magicules |
| "Only a witch, siphoner, heretic or tribrid can break a bond." / "The rite needs Sire's Blood." / "That is not the blood of their sire." / "Nobody sired them: there is no bond to break." / "Their bond is already broken." | the Rite of Severance: see [its page](Rite-Of-Severance). It also needs the **night** of a full moon. |
| The apex race is shown but the pick does nothing | `apex_evolutions_enabled` differs between client and server (the server refuses silently) |

See [Rites](Rites). A rite that needs Werewolves doesn't exist without it.

## A werewolf can't shift

With `moon_bound_werewolves` on (the default), a werewolf can only shift **when the full moon forces it**, unless they have **moon control**: an Alpha or True Alpha, a **Moon Ring** bound to them, an unbound hybrid, or a born wolf with an anchor (half only). See [moon control](Werewolves-And-Packs#moon-control). Also check:

- an **eclipse** is running: no form actions at all until dawn;
- they are **Shocked**, or hit by **wolf lichen**;
- a **bound hybrid** needs the Rite of the Moon.

## A bite didn't turn someone

Only an **Alpha's** bite turns a human, with a 10% chance of killing instead. Betas and Omegas don't infect. Vampires, hunters and some species can't be turned. A target who already has a pack stays in it.

## There is no apex race in the evolution screen

The player must have a **species**, a human-like race at **saint tier or higher**, `apex_evolutions_enabled` on **on the client too**, and `species_tensura` on. If the log said the evolution hook is `MISSING`, Tensura or ManasCore was updated and the hook didn't find its target.

## A layer skill or bonus is missing

- Is `species_tensura` on, on both sides?
- A bad entry in `layers` is **ignored**: operators are told in chat at login and the log has an ERROR line.
- A skill the player already had is not tracked by the mod and is never taken or given twice.
- After the species was lost the layer skills are **dormant**; they come back with the species.

## A ring does nothing

Is it **bound** to you (tooltip "Bound to ...")? A ring bound to someone else, or unbound, does nothing. The **Sun Ring** is for vampires and the **Moon Ring** for werewolves; the **Resurrection Ring** only works for human-side players against supernatural killers. Is it in a Curios `ring` slot, or in your inventory?

## A vampire still burns, or can't bite

- A faction vampire's sun damage is Vampirism's. A [Sun Ring](Items-And-Blocks#rings-and-wards) bound to them blocks it.
- "There is no blood in them to drink." means the victim's Tensura race is bloodless (a slime, a ghoul or a spirit, for example). Apex races are never bloodless.

## A sired player can't hurt their sire

That is the Tensura subordinate link, and for hybrids rule R43. `sire_subordinate_link_enabled = false` removes the first. The **[Rite of Severance](Rite-Of-Severance)** frees one player from both. See [Tensura integration](Tensura-Integration#sires-as-tensura-owners-and-naming).

## Blocks or items disappeared

A part was switched off, or a mod it needs was removed. Unknown ids are dropped when the world loads. Put the part back **before** loading, or restore a backup. See [For server owners](For-Server-Owners#switching-parts-off).

## Reporting a bug

[Open an issue](https://github.com/OFA-Tech/SupernaturalBloodlines-MinecraftMod/issues) with:

- the mod version and the versions of Vampirism, Werewolves, Enchanted, Tensura, ManasCore and Curios, and NeoForge;
- the `Part X: ...` lines from the log, and the config files;
- what you did, and what you expected;
- the output of `/supernaturalbloodlines debug get <player>` for the player (operators only);
- the relevant lines of `latest.log` (and the crash report if there is one).

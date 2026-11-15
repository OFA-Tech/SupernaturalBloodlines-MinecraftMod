# Ability HUD

The ability HUD (plan 13) is being built in phases. So far only the **Bite action** exists; the six slots, the Tensura and Supernatural modes and the Loadout tab come with the next phases.

## The Bite action

Bite is **not a key**. It is an action that sits in a HUD slot (Supernatural-mode slot 1, so **R**, by default) and does the right thing for what you are. It needs **Vampirism**. The server decides everything: the target must be in the same world, within 4 blocks (`bite_reach`), in sight and alive. One bite per second. A refusal always says why in the action bar.

| You are | Bite does |
|---|---|
| A werewolf, hybrid, Original hybrid or tribrid **in a wolf form** | The Werewolves bite: turns humans and poisons vampires |
| A Vampirism vampire (a Ghoul too) | Vampirism's blood drinking. Hold the key to keep drinking, let go to stop |
| A hybrid, Original hybrid or tribrid **in human form** | Feeds you: +5 blood thirst. The victim takes 2 damage and loses 2 blood, so your venom still poisons a vampire, and a victim with no blood left gives nothing. Refused at full thirst, and where a claim or PvP protection protects the victim |
| A human in the [turning](Vampires-And-Originals#the-turning) | Bite a villager, trader, hunter or human-like player to finish the turning |
| A werewolf in human form with no vampire side | The Werewolves bite, which refuses while your form cannot bite |
| Anyone else | Nothing. A Tensura vampire race that is not a Vampirism vampire feeds with Tensura's own race ability key |

Vampirism's own bite key and Werewolves' bite key are covered by the HUD in a later phase.

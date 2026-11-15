# Ability HUD

The ability HUD (plan 13) is being built in phases. So far only the **Bite action** exists; the six slots, the Tensura and Supernatural modes and the Loadout tab come with the next phases.

## The six slots

Six slots (keys **R F V** and **Z X C** by default, see [Key binds](Key-Binds)): slots 1 to 3 are the left column and 4 to 6 the right one.

- **Tensura mode** (needs Tensura): slots 1 to 3 are your active Tensura preset and 4 to 6 the **next** preset, like Tensura Slots (preset 9 pairs with preset 1). A slot key does what Tensura's own key does: press, hold and release, toggle skills, and **hold Alt with a slot key to change that skill's mode**. Alt + scroll still moves through the presets, as in Tensura. You no longer need Tensura Slots. **Note for server owners:** the right column works through one small mixin that applies to **every player on the server**: the skills in the next preset count as equipped for their passives and for activation, so everybody has 6 equipped skills instead of 3, exactly as with Tensura Slots. Set `next_preset_active = false` in `supernaturalbloodlines-hud.toml` to turn it off (the right column then does nothing, as Tensura itself refuses those skills).
- **Supernatural mode** (needs Vampirism): the six slots are the actions you put in the hub's Loadout tab, and **Bite** (slot 1 by default). A slot key does what Vampirism's own key for that action does (a lasting action switches on and off, a short one fires) and the server checks that you have the action unlocked; a refusal says why in the action bar. Holding the key of a Bite slot keeps a vampire drinking. A werewolf or hybrid's slots hold Werewolves' actions. You can put an action in several slots. Slots the hub does not fill stay empty: with a faction change the list is kept, and the slots you can no longer use are shown greyed.
- **Alt + Caps Lock** switches between the two when both are installed.

## What is on screen

Two columns of three boxes stand on the bottom edge, one left of the offhand slot and one right of the attack indicator. Each box shows the skill or action icon, its key in the corner, a dark fill with the seconds left while it recharges, a gold outline while it is on, and a faded icon when you cannot use it. **Hold Alt** to read a name (and the mode, for a Tensura skill that has modes) and why a box is greyed or how long it has left. **Alt + Caps Lock** switches between Tensura and Supernatural mode when you have both; the action bar says which one you are in, and the choice is kept for next time. While the HUD is on, Tensura's own skill HUD is hidden (turn that off with `hide_tensura_skill_hud`); open Tensura's HUD settings and you see your own setting, and it comes back when you leave.

A slot key also fires when Ctrl or Shift is held with it (Alt is how you change a skill's mode), and a press in the very moment a screen closes is ignored, so a slot key that is also your inventory key never fires as the inventory closes. On a narrow window at a big GUI scale a column that does not fit moves up above the hotbar.

## Blood

A Vampirism vampire has one blood value, Vampirism's. With **Tensura installed** it is shown in Tensura's **food spot**: our blood drop and the number, in the place, size and side that Tensura gives its own food number, and Vampirism's blood bar is hidden. Tensura's food number comes back by itself when you are not a vampire, and both come back if you set `blood_in_food_spot = false` in `supernaturalbloodlines-hud-client.toml`. Nothing is deleted: the two displays are only hidden while ours is on, and Tensura's own setting is put back exactly as you had it (also when you open Tensura's HUD settings or leave the world). **Without Tensura**, Vampirism's blood bar stays where it is. If an update of Tensura changes the parts we reach, the blood simply stays in Vampirism's bar and the log says so once.

## Putting actions in the slots

In the [Supernatural hub](Supernatural-Hub), the **Skills** page has a **Slots** tab (only when this part is installed). The left page lists Bite and every action you have unlocked; the right page shows the six slots with their keys. **Click an action and then a slot** (or a slot and then an action) to put it there; the small **x** empties a slot. You can put one action in several slots. Without the supernatural part there is no hub, so slots 2 to 6 show your unlocked actions in order, and Bite stays in slot 1.

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

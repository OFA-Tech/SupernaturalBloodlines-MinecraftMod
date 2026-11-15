# Key binds

The mod's key binds are on the client only. First the Alpha Roar and the hub; the [HUD keys](#the-hud-keys-hud-part) are below.

| Key bind | Category in Controls | Default | Does |
|---|---|---|---|
| **Supernatural hub** | Supernatural Bloodlines | **Home** | Opens the [Supernatural hub](Supernatural-Hub); in the hub it closes it. Only exists with the supernatural part on. J was not used: it is JourneyMap's map, and = and - are map zoom keys. |
| **Alpha Roar** | Supernatural Bloodlines | **unbound** (registered with no default key; only exists when **Werewolves** is installed) | The [Alpha Roar](Werewolves-And-Packs#alpha-roar). Only an Alpha or True Alpha can use it; the server checks. Same as `/pack roar`. |

Bind it in **Options > Controls > Key Binds**, under **Supernatural Bloodlines**.

## The HUD keys (hud part)

All in the **Supernatural Bloodlines** category, in game only.

| Key bind | Default | Does |
|---|---|---|
| **Slot 1, 2, 3** | **R, F, V** | The left column of the [ability HUD](Ability-HUD). Slot 1 is where the Bite action sits by default |
| **Slot 4, 5, 6** | **Z, X, C** | The right column |
| **HUD mode** | **Alt + Caps Lock** | Switches between Tensura mode and Supernatural mode. Caps Lock alone is not ours |
| Supernatural hub | **Home** | (above, supernatural part) |

There is **no Bite key**: Bite is an action in a slot. The slots do nothing until the HUD arrives in the next phase.

### Covered keys, and how to get them back

One keymap means the keys of the other mods that the HUD replaces would only clash with it. With the part on and `hide_covered_keys` on (the default), the first time you start the game these keys are **set to unbound and hidden from Controls** (also from Controlling's list and its search):

- Tensura's Ability 1 to 3 activation
- Tensura Slots' Ability 4 to 6 activation
- Vampirism's Suck blood and Toggle action 1 to 9
- Werewolves' Bite

Nothing is deleted. Your bind for each is first written to `config/supernaturalbloodlines-hud-keys.json`. Set `hide_covered_keys` to `false` in `supernaturalbloodlines-hud-client.toml` (or turn the `hud` part off) and the recorded binds come back, **your own, not the mod default**, unless you have bound that key to something else meanwhile (then yours stays). If you used "Reset All" in Controls, the covered keys are unbound again when you close it. These keys stay with their owners and are never touched: Tensura's Next/Previous Ability Mode (Alt, and Alt + scroll for presets), Race Ability and Status Menu, and Vampirism's action wheel, vision and skill screen.

The server checks the roar itself: a press from someone who isn't an Alpha, or during a **lunar eclipse**, or during the **60 second** cooldown, does nothing but show a message in the action bar ("Only an Alpha can do that.", "You cannot roar for another N seconds.", "The eclipse swallows your roar.").

## Actions that use ordinary keys

These are not key binds, only things you do with the normal keys:

| Action | What you do | Who |
|---|---|---|
| **Compulsion** | **Sneak** + **right-click** with an **empty main hand** on a villager or a lesser vampire, within 4 blocks, eye to eye | Original, hybrid types, or a vampire of level 10+ |
| **Siphon** | **Sneak** + **right-click** with an **empty main hand** on a block with altar power, or on a creature | siphoner, heretic, tribrid |
| **Drink blood** | **Right-click** a Vampirism blood bottle | a hybrid (adds thirst; a werewolf-faction player can't use Vampirism's bottle) |
| **Pull the dagger** | **Right-click** a desiccated Original | any other player |
| **Hearing Device** | **Right-click** | whoever holds it |
| **Accept a call or an invitation** | click the **[Accept]** button in chat | the sired, or the invited werewolf |
| **Choose a compulsion** | click **[Discount]** or **[Follow]** in chat | the caster |
| **Downed screen** | click **Give up** or **Wait** | an Original hybrid at lethal damage |

# Key binds

The mod's key binds are on the client only. First the hub and the Alpha Roar (which the HUD hides, see below); the [HUD keys](#the-hud-keys-hud-part) are below.

| Key bind | Category in Controls | Default | Does |
|---|---|---|---|
| **Supernatural hub** | Supernatural Bloodlines | **Home** | Opens the [Supernatural hub](Supernatural-Hub); in the hub it closes it. Only exists with the supernatural part on. J was not used: it is JourneyMap's map, and = and - are map zoom keys. |
| **Alpha Roar** | Supernatural Bloodlines | **unbound**; always registered when **Werewolves** is installed, but **hidden and unbound while the HUD works** (the Roar is then an action in a slot; the key comes back with `hide_covered_keys` off, with the HUD off, or on a server without the HUD) | The [Alpha Roar](Werewolves-And-Packs#alpha-roar) for a game or server without the HUD. Only an Alpha or True Alpha can use it; the server checks. Same as `/pack roar`. |

Bind it in **Options > Controls > Key Binds**, under **Supernatural Bloodlines**.

## The HUD keys (hud part)

All in the **Supernatural Bloodlines** category, in game only.

| Key bind | Default | Does |
|---|---|---|
| **Slot 1, 2, 3** | **R, F, V** | The left column of the [ability HUD](Ability-HUD). Slot 1 is where the Bite action sits by default |
| **Slot 4, 5, 6** | **Z, X, C** | The right column |
| **HUD mode** | **Caps Lock** | Switches between Tensura mode and Supernatural mode (a Caps press with Alt, Ctrl or Shift held also works). **Upgrading:** a game that has run an earlier version (v0.2.0) keeps the HUD mode key it saved, Alt + Caps Lock; a new install gets plain Caps Lock. To switch, set "HUD mode" in Controls (or delete the `key_key.supernaturalbloodlines.hud_mode` line from `options.txt` while the game is closed). If another mod uses Caps Lock, rebind one of them in Controls; in the OFA pack the held radio moved to Backslash |
| Supernatural hub | **Home** | (above, supernatural part) |

There is **no Bite key**: Bite is an action in a slot. The slots do nothing until the HUD arrives in the next phase.

### Covered keys, and how to get them back

One keymap means the keys of the other mods that the HUD replaces would only clash with it. With the part on, `hud_enabled` on and `hide_covered_keys` on (the defaults), **while you are in a world whose server has this mod's HUD** (your own world, or a server with the `hud` part on) these keys are **set to unbound and hidden from Controls** (also from Controlling's list and its search):

- Tensura's Ability 1 to 3 activation
- Tensura Slots' Ability 4 to 6 activation
- Vampirism's Suck blood and Toggle action 1 to 9
- Werewolves' Bite

Nothing is deleted. Your bind for each is first written to `config/supernaturalbloodlines-hud-keys.json`. They come back by themselves **when you leave the world** (the title screen shows them normally), when you join a **server without the mod** or with its `hud` part off, and when you set `hud_enabled` to `false` (even with `hide_covered_keys` on). Setting `hide_covered_keys` to `false` in `supernaturalbloodlines-hud-client.toml`, or turning the `hud` part off, brings them back too. The recorded binds come back, **your own, not the mod default**, unless you have bound that key to something else meanwhile (then yours stays). The record is written first and replaced in one step (a `.tmp` file moved over it), so a crash cannot leave half of it, and if it cannot be saved no key is touched. **If the record cannot be read** (for example you edited it by hand and broke the JSON), it is **kept**, not overwritten: it is renamed `supernaturalbloodlines-hud-keys.json.bad-<time>` (a second one gets its own name), the log says so, and **nothing is covered while any `.bad-*` file exists** (your keys stay as they are and visible). To recover by hand: open the newest `.bad-*` file, read the key id and bind of each line (for example `"tensura.keybinding.ability.slot_1": "key.keyboard.z"`), put those binds back in Controls or in `options.txt` (the `key_<id>:<bind>` lines), then delete the `.bad-*` files and restart; the next start covers the keys again and writes a fresh record. If you turn the `hud` part off, the recorded binds still come back.

If you used "Reset All" in Controls, the covered keys are unbound again when you close it. **Removing the mod:** the restore code is in the jar, so **start the game once with `hide_covered_keys = false` (or `hud_enabled = false`, or just sit at the title screen) before you delete the jar**. If the jar is already gone, the keys stay unbound: bind them again in Controls (the old binds are listed in `config/supernaturalbloodlines-hud-keys.json`). These keys stay with their owners and are never touched: Tensura's Next/Previous Ability Mode (Alt, and Alt + scroll for presets), Race Ability and Status Menu, and Vampirism's action wheel, vision and skill screen.

The server checks the roar itself (from a slot, the key or the command): a use by someone who isn't an Alpha, or during a **lunar eclipse**, or during the **60 second** cooldown, does nothing but show a message in the action bar ("Only an Alpha can do that.", "You cannot roar for another N seconds.", "The eclipse swallows your roar.").

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

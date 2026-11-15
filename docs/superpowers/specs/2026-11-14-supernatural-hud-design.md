# Plan 13: the Supernatural HUD, keys and blood (design, 2026-11-14)

## Why

Players should go straight to playing. Today, a player with Tensura, Tensura Slots, Vampirism and Werewolves has:
- two skill HUDs (Tensura's left column and Tensura Slots' right column);
- Vampirism's action wheel;
- two feeding systems with two bars;
- several bite keys;
- hundreds of keybinds that clash.

The owner wants **one HUD (with one Bite action in it), one blood value and one keymap**. It has to be seamless: every mod keeps its own data and screens, and nothing a mod does is lost.

## Goals

- **One 6-slot ability HUD.**
  - Its slots are filled from Tensura skills (preset pairs) or from Vampirism/Werewolves actions.
  - **Alt + Caps Lock** switches between the two sources when both are installed.
  - It replaces Tensura's skill HUD and the Tensura Slots mod.
- **One Bite action, in the HUD** (owner, 2026-11-15: "this should be in the skills hud, like the rest"). It does the right thing for what you are. It is not a separate key: by default it sits in Supernatural-mode slot 1 (R), and the hub's Loadout tab can move it.
- **One blood value**, Vampirism's, shown in one place.
- **One keymap with no clashes** across Tensura, Vampirism, Werewolves, Enchanted and this mod. The covered keys of the other mods are set to unbound and hidden automatically.
- **Player-ready defaults**, shipped the same way the OFA pack ships its defaults.

## Non-goals

- Changing any mod's skills, actions, cooldowns or data. The HUD reads them and triggers them; it never stores or changes them.
- Removing anything from Tensura's menus. Tensura's preset screen stays the place to set up Tensura presets.
- The Supernatural hub and the vampire-feeding skill (plan 12).

## Anti-goals

- A HUD that drifts out of step with a mod after an update. Every slot reads straight from its owner each frame.
- Deleting another mod's keybind. Hidden keys come back as soon as our HUD or keymap is turned off.
- Clutter when a mod is missing. Whatever isn't installed simply isn't shown.

## Behaviour

### The ability HUD

**Layout.** Two columns of 3 slots, styled like the vanilla hotbar (see `supernatural-hud-v2.html` in the brainstorm):
- Left column: R / F / V.
- Right column: Z / X / C.

Each slot shows:
- the icon;
- the key, in the corner;
- the cooldown, as a darkening plus a number;
- an outline while the ability is active.

**Tensura mode.** The left column is the current Tensura preset and the right column is the next preset, as Tensura Slots does today. **Alt + scroll** moves through the preset pairs. The slots trigger through Tensura's own skill activation.

**Supernatural mode.** The 6 slots hold Vampirism/Werewolves actions that the player picked, plus our **Bite** action. The pick is stored by us as a list of 6 ids per player. A new player starts with Bite in slot 1 and the rest empty. The player assigns slots in the **Loadout tab of the plan 12 hub** (Home key). There is no separate assignment screen. The action slots trigger through Vampirism's action API, and cooldowns and active state come from it. The Bite slot triggers the Bite action (below). This replaces using the action wheel, but the wheel itself stays available.

**Switching.** **Alt + Caps Lock** toggles the mode when both Tensura and Vampirism are installed. With only one installed, the HUD shows only that mode. With neither, it doesn't render.

**Alt held.** Shows name, mode and cooldown for each slot in vanilla-tooltip panels.

**Hidden.** Tensura's own skill HUD is hidden. Tensura Slots is removed from the pack: the HUD covers it, and our 6 slots replace its 3.

### The Bite action

Bite is one of the HUD's actions, not a key of its own. It fires through the key of whatever slot it is in (slot 1, R, by default). When it fires, a decision table picks what the bite means. Bite is offered only in Supernatural mode, so only with Vampirism installed. A Tensura vampire's own race-ability feeding stays on Tensura's `race_ability` key (`[` in the OFA pack) and is not touched.

**R110, decision table (owner, 2026-11-14: "by form"; hybrid ruling 2026-11-15), first match wins:**

| # | Player | Bite does |
|---|---|---|
| 1 | Werewolf, hybrid, Original hybrid or tribrid **in a wolf form** | Werewolves bite: turns humans (R92) and poisons vampires (R42) |
| 2 | Vampirism vampire (a Ghoul too) | Vampirism blood drinking |
| 3 | Hybrid, Original hybrid or tribrid **in human form** | **Our own feeding**: fills the hybrid blood thirst (plan 04's `thirst`, +5 per bite like a blood bottle), and hybrid venom still poisons a vampire victim (R42) |
| T | Human in plan 11's turning (`turningTicksLeft > 0`, not yet a vampire) | Bites a **human-like** target (plan 11's rule) and completes the turning, like drinking a blood bottle does |
| 4 | Werewolf in human form, with no vampire side | Werewolves bite |
| 5 | Anyone else (a Tensura vampire race with no Vampirism faction feeds with Tensura's own key) | Nothing; the slot shows greyed with the reason |

Hybrids are in the Werewolves faction, not Vampirism's vampire faction, so Vampirism's drinking can't serve them. In human form they feed like a vampire through our own feeding (row 3). A Tensura vampire who is also a Vampirism vampire takes row 2, because it fills the same blood.

### One blood value

- Vampirism's blood level is the single value.
- With Tensura installed, Vampirism's blood-drop bar is hidden. A Vampirism vampire's blood shows in Tensura's HUD food spot instead.
- For a Vampirism vampire, feeding through Tensura's race ability or through the Bite action fills Vampirism blood. A hybrid's Bite in human form fills the hybrid thirst instead (R110 row 3).
- Non-vampires keep normal food.

**Human-like blood** includes villagers and other Vampirism human creatures (owner, 2026-11-14). The same rule applies to plan 11's turning.

### One keymap

- A full default keymap covers Tensura, Vampirism, Werewolves, Enchanted and this mod, starting from the owner's current binds: R F V and Z X C for skills, Alt + scroll, Alt + Caps Lock, and **Home for the hub**. There is no Bite key.
- Every other mod key that our HUD or keymap replaces is set to unbound and hidden from the Controls screen automatically, with a config switch to bring them all back. This covers Tensura's three slot keys, Tensura Slots' keys, Vampirism's suck and quick-action keys, and the Werewolves bite key.
- The pack's `apply_keymap.py` / `configureddefaults` stay the shipping route for pack defaults.
- **OFA pack (2026-11-15).** Vampirism, Werewolves and SupernaturalBloodlines are deliberately out of the pack until this mod is done; the owner adds all three back at once. The pack work prepares the keymap and `configureddefaults` for that moment, without installing them now. It also fixes the stale `KEYMAP.md` and `apply_keymap.py` (the live binds are R/F/V Tensura, Z/X/C Tensura Slots, Left Alt next mode, `[` race ability) and the F clash between swap-offhand and Tensura slot 2 in `configureddefaults`.

### Defaults

Player-facing settings ship ready to play: HUD on, the mode that matches the installed mods, the keymap applied, and hidden duplicates.

## Answers from the builder check (designer calls, marked)

- **Tensura mode slot rules.** These come from the owner's screenshots and the Tensura Slots jar in the pack (`mods/tensuraslots-1.0.0.jar`, which the builder may read):
  - Left column = preset N, right column = preset N+1.
  - Alt + scroll moves N by 1. Preset 9 pairs with preset 1 *(call)*.
  - An empty preset slot shows an empty slot.
- **Skill input parity.** Slots forward press, hold and release, and toggle/mode-cycle, exactly as Tensura's own keys do. Tensura's held abilities must keep working. Cooldown, active state and mode are read from Tensura's skill instance on the client. If that data isn't available on the client, the builder syncs it. *(call: "all features kept" requires this)*
- **Human-like blood governs** plan 11's turning completion (by blood bottle or by Bite, row T) and plan 12's "drink human blood" tasks. Normal feeding values stay Vampirism's own. A Tensura race-ability feed credits the same Vampirism blood as a Vampirism bite on that target would. *(call)*
- **Supernatural mode for hybrids and others.**
  - The assignable actions are those the player has unlocked in their current Vampirism/Werewolves faction. A hybrid is a Werewolves-faction player, so they get Werewolves actions; their vampire-side abilities come from plan 12.
  - A slot whose action is locked or unusable in the current form shows greyed, with the reason in the Alt tooltip.
  - The list persists across death and is never auto-cleared. *(call)*
- **Hidden keys.**
  - Hiding from the Controls screen may use a mixin in this plan's own new part (budget 3).
  - Before unbinding, we record each key's current bind, and turning the switch off restores the player's own earlier binds, not just the mod defaults. *(call)*
  - The covered key names come from each mod's jar.
- **Without Tensura,** Vampirism's blood bar stays as it is. *(call)*

## Approaches

| Option | What it means | Trade-off |
|---|---|---|
| **A. Preset pairs plus a picked action list (chosen)** | Reuses Tensura's presets. Stores only a 6-id list for the Supernatural side | Seamless. Tensura's own preset screen remains the setup for Tensura mode. |
| B. Our own 6-slot loadouts for everything | One storage for all skills | A second source of truth next to Tensura's presets. Rejected by the owner as not seamless. |
| C. Mixing any mod's ability in any slot | Maximum freedom | Slots go out of step when mods update. Rejected for being fragile. |

## Left to the builder

- How to hide Tensura's skill HUD: prefer cancelling its GUI layer, and only use a mixin if no layer exists.
- How to render blood in Tensura's food spot.
- The full default keymap table.
- The Loadout tab's exact layout inside the hub.

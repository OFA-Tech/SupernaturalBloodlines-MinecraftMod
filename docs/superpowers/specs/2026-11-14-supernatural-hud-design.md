# Plan 13: the Supernatural HUD, keys and blood (design, 2026-11-14)

## Why

Players should go straight to playing. Today, a player with Tensura, Tensura Slots, Vampirism and Werewolves has:
- two skill HUDs (Tensura's left column and Tensura Slots' right column);
- Vampirism's action wheel;
- two feeding systems with two bars;
- several bite keys;
- hundreds of keybinds that clash.

The owner wants **one HUD, one Bite key, one blood value and one keymap**. It has to be seamless: every mod keeps its own data and screens, and nothing a mod does is lost.

## Goals

- **One 6-slot ability HUD.**
  - Its slots are filled from Tensura skills (preset pairs) or from Vampirism/Werewolves actions.
  - **Alt + Caps Lock** switches between the two sources when both are installed.
  - It replaces Tensura's skill HUD and the Tensura Slots mod.
- **One Bite key** that does the right thing for what you are.
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

**Supernatural mode.** The 6 slots hold Vampirism/Werewolves actions that the player picked. The pick is stored by us as a list of 6 action ids per player. Until plan 12's hub exists, the player assigns actions on a small assignment screen opened from the HUD, and plan 12 later moves it into the hub. The slots trigger through Vampirism's action API, and cooldowns and active state come from it. This replaces using the action wheel, but the wheel itself stays available.

**Switching.** **Alt + Caps Lock** toggles the mode when both Tensura and Vampirism are installed. With only one installed, the HUD shows only that mode. With neither, it doesn't render.

**Alt held.** Shows name, mode and cooldown for each slot in vanilla-tooltip panels.

**Hidden.** Tensura's own skill HUD is hidden. Tensura Slots is removed from the pack: the HUD covers it, and our 6 slots replace its 3.

### The Bite key

One key decides what a bite means:
- **A Tensura vampire-line race:** Tensura's race ability (feeding).
- **A Vampirism vampire:** Vampirism's blood drinking.
- **A werewolf or a hybrid:** the Werewolves bite (venom rules apply).

**Decision table (owner, 2026-11-14: "by form"), first match wins:**

| # | Player | Bite does |
|---|---|---|
| 1 | Werewolf, hybrid, Original hybrid or tribrid **in a wolf form** | Werewolves bite: turns humans (R92) and poisons vampires (R42) |
| 2 | Any Vampirism vampire in human form, hybrids included | Vampirism blood drinking. Hybrid venom still poisons a vampire victim (R42) |
| 3 | Werewolf in human form, with no vampire side | Werewolves bite |
| 4 | Tensura vampire-line race, **with Vampirism not installed** | Tensura race ability (feeding) |
| 5 | Anyone else | Nothing happens |

A Tensura vampire who is also a Vampirism vampire always takes row 2, because it fills the same blood.

### One blood value

- Vampirism's blood level is the single value.
- With Tensura installed, Vampirism's blood-drop bar is hidden. A Vampirism vampire's blood shows in Tensura's HUD food spot instead.
- Feeding through Tensura's race ability, or through our Bite key, fills Vampirism blood.
- Non-vampires keep normal food.

**Human-like blood** includes villagers and other Vampirism human creatures (owner, 2026-11-14). The same rule applies to plan 11's turning.

### One keymap

- A full default keymap covers Tensura, Vampirism, Werewolves, Enchanted and this mod, starting from the owner's current binds: R F V and Z X C for skills, Alt + scroll, Alt + Caps Lock.
- Every other mod key that our HUD or keymap replaces is set to unbound and hidden from the Controls screen automatically, with a config switch to bring them all back.
- The pack's `apply_keymap.py` / `configureddefaults` stay the shipping route for pack defaults.

### Defaults

Player-facing settings ship ready to play: HUD on, the mode that matches the installed mods, the keymap applied, and hidden duplicates.

## Answers from the builder check (designer calls, marked)

- **Tensura mode slot rules.** These come from the owner's screenshots and the Tensura Slots jar in the pack (`mods/tensuraslots-1.0.0.jar`, which the builder may read):
  - Left column = preset N, right column = preset N+1.
  - Alt + scroll moves N by 1. Preset 9 pairs with preset 1 *(call)*.
  - An empty preset slot shows an empty slot.
- **Skill input parity.** Slots forward press, hold and release, and toggle/mode-cycle, exactly as Tensura's own keys do. Tensura's held abilities must keep working. Cooldown, active state and mode are read from Tensura's skill instance on the client. If that data isn't available on the client, the builder syncs it. *(call: "all features kept" requires this)*
- **Human-like blood governs** plan 11's turning completion and plan 12's "drink human blood" tasks. Normal feeding values stay Vampirism's own. A Tensura race-ability feed credits the same Vampirism blood as a Vampirism bite on that target would. *(call)*
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
- The assignment screen's exact layout.
- The priority order for the Bite key. Check it against the rule tables.

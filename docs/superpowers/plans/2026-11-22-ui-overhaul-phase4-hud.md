# UI overhaul phase 4: the HUD rebuilt on the kit

> **For agentic workers:** execute task by task with TDD; every test must fail when its logic is reverted.

**Goal:** the ability HUD (six slots, Alt labels, mode pill, other-mode panel, moon warning, tracker) and the request toast draw the phase 2 kit instead of hand-filled shapes, in the look of the hub's theme.

**Inputs:** roadmap phase 4, `design/ui-overhaul/phase1/hud-*.png`, the phase 2 kit (`slot`, `slot_active`, `slot_square`, `slot_square_active`, `key_badge`, `key_badge_wide`, `hud_pill`, `hud_label`, `mastery_track`, `mastery_fill*`, `shared/cooldown_sweep`, `shared/blood_ring`, `shared/mode_mark`, 16 px icons).

## Kept as shipped in PR #56 (each has a test that stays green)

- Alt labels on the outer side of each column, two lines, centred on the slot's row, state first; a blocked slot has a third line.
- The mastery bar along the bottom edge; the key badge and the seconds are lifted over it.
- Keys R F V / Z X C.
- The mode pill only on a switch or while Alt is held; the HUD fades when idle.
- Tensura mode: square slots in every theme, marked with a teal diamond.
- Labels never overlap a slot, the pill, the other-mode panel or the hotbar (`HudLayoutTest`, `HudPanelsTest`).

## Tasks

1. **Kit parts for the HUD** (`KitSprites.Part`): slot frames, key badges, mastery track and fills, pill, label. `SCALED` holds the nine-slice ones. Flat stand-ins exist for each, so a pack that removes a sprite still gets a usable HUD.
2. **`KitColors`**: the colours the kit's drawing needs, split out of `HubTheme`. `HudKit` is the HUD's three looks, held equal to the hub themes by a test. `UiKit` takes `KitColors`.
3. **Frame strips** (`FrameStrip`, `UiKit.frame`): the cooldown sweep (33 frames of 16 px) and the blood ring (25 frames of 32 px) replace the baked masks. A strip of the wrong size (a hi-res pack) draws a flat dim instead.
4. **`SlotArt`** from the kit: frame by mode (`framePart`), icon (our own Bite and Alpha Roar use the kit's 16 px icons, Tensura's keep their textures), sweep, seconds, lock, mastery track and per-state fill, badge (11 or 17 px wide by label), blood ring, mode mark.
5. **`HudChrome`** from the kit: the pill (the Grimoire's is drawn 16 px tall so its capitals fit between the gold lines), the label box for the Alt labels, the other-mode panel, the tracker (with the mastery track as its progress), and the moon warning with the `moon_full` icon.
6. **Text on pictures** (`HudFaces`): the letter, the name, the body and the warning colours, each checked at 4.5:1 on every pixel of the picture it stands on (`HudContrastTest` reads the PNGs). Mastery fills are checked at 3:1 on their track.
7. **Ready glow** (`ReadyGlow`): a slot whose cooldown has just run out shows the active frame for one 30 tick loop. A flourish, so `reduce_motion` switches it off. The active frame's pulse was already behind `reduce_motion` (`Motion.pulse`), and stays so.
8. **Request toast** (`ToastPlace`): the toast uses the kit's label box. When Tensura or Vampirism is installed, it stands under the status they draw top left (`TrackerAnchor.CORNER_H`), so it never covers it.
9. **Dev shots**: `hud:<look>[:alt|human|humanalt|cooldown|ready|humanready|toast|humantoast]`, with the chat cleared first so the world's messages do not sit on the slots.

## Not in this phase

- Boss bars and the hub pages (phases 5 and 6).
- The Tensura mod's own status HUD is not ours and is not restyled.

## Verification

`./gradlew.bat test`, the 12 GameTest runs one at a time (`--max-workers=2`), and hidden shots of every look at scales 2 and 4 in both modes, with Alt held, a cooldown, a greyed slot, a mastered and a learning skill, the ready glow and the toast with the Tensura HUD. Every shot opened and checked for overlaps, cut text and contrast.

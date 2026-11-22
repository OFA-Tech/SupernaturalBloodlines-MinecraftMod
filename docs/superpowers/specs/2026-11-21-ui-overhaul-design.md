# UI overhaul: hub, HUD, boss bars, transformations, logo

Owner request, 2026-11-21: "make it modernized and super clean and nicely built. Not just a bunch of texts and poor pixelated stuff. No AI slop." The owner chose approach A and keeps all three themes.

## Intent

Today the hub and HUD are almost all drawn in code: flat fills, line rings, ink sigils, and one sprite sheet of 36 icons. They work, are tested and are readable, but they look like a debug screen. The owner wants the mod to look crafted, so that a player opening the hub feels it was made with care.

## Owner's answers

- **Art direction:** crafted, high-detail pixel art. Icons are 32 px, mostly 64 px where large. Frames, emblems and ornaments are hand-made. This is not smooth or painterly art, and not AI imagery.
- **References they love:** Cobblemon (clean panels, smooth motion), Create and Ponder (brass and wood, built and readable), and Genshin and Hollow Knight menus (ornament, glow, motion with weight).
- **Motion:** purposeful only. Page turns, a pop when a slot is pressed, cooldown sweeps, an unlock flourish, a glow when ready, boss bar changes. There is a "reduce motion" setting.
- **Boss bars:** a portrait and name plate, a form indicator (human/wolf, Host/Beast/Spirit) that changes during a transformation, and a frame in the hub theme.
- **Transformations:** real morph animations for Klaus, Deucalion, Peter and the Beast. These are GeckoLib sequences made in Blockbench by Fable, 1.5–2.5 s long, with the boss vulnerable during them, plus particles and sound.
- **Branding:** a logo, a mod icon and a store banner, all one consistent mark.
- **Themes:** keep Grimoire, Blood Moon and Tensura.

## Goals

1. **One UI kit per theme, as real textures:**
   - nine-slice frames, panels, buttons (normal, hover, pressed, disabled), tabs, scroll bars and dividers;
   - emblems, seals, gems and ornaments;
   - 32 px icons for every skill, action, page, rank, gene and status.

   The code draws the kit and never hand-fills a shape the kit has.
2. **Every hub page and the HUD are rebuilt on the kit**, screen by screen, each with screenshots at GUI scales 2/3/4 in the three themes.
3. **A small motion system:** eased tweens on the client tick for scale, alpha, offset and glow. Page transitions, press feedback, unlock and claim flourishes, ready glow. Everything turns off with "reduce motion".
4. **Custom boss bars:**
   - themed frame, portrait for each form, name plate and form indicator;
   - several bosses at once still read clearly.

   These replace vanilla's bar for our bosses only.
5. **Transformation animations** for the four multi-form bosses, with server timing that matches the animation.
6. **Logo, mod-list icon and store banner.**

## Non-goals

- New features or pages. This is a visual overhaul; what each page does stays the same.
- Changing other mods' screens. Tensura's, Enchanted's, Vampirism's and Werewolves' screens stay as they are.
- Changing vanilla boss bars for other mods' bosses.

## Anti-goals (the overhaul fails if any of these happen, even if it works)

- Generic AI-generated art, smooth gradients pasted onto a pixel font, or mixed pixel sizes on one screen.
- Losing readability: text contrast stays at least 4.5:1, focus rings stay, narration stays, and nothing is cut off at scale 4.
- Motion that slows play or cannot be turned off.
- Three themes that look like one theme recoloured. Each keeps its identity (book / moon / status window) from plan 16.

## Constraints

- **Pixel grid:** Minecraft's font and GUI scale (2–4). Art must sit on a consistent pixel grid: one texel is one GUI pixel. Larger art is drawn at that same density, never scaled by fractions.
- **Accessibility:** the existing tests stay green and grow to cover the kit (`Contrast`, `LabelFit`, `FocusWalk`, narration).
- **Performance:** no per-pixel fills per frame. The kit is texture blits, batched. The existing performance rules from plan 16 apply.
- **Art pipeline:**
  - Fable 5.2 agents make all art (project rule #1135).
  - Gather references before drawing (#1136).
  - Pixel art is drawn by hand: Blockbench texture painting, or exact pixel scripts reviewed by eye. No image-generation models.
  - Every asset is checked at its real size in-game before it is accepted.
- **Licences:** our own art only. References are for inspiration and are recorded in REFERENCES.md.
- **No new mixins** unless a boss bar replacement needs one. Prefer NeoForge's `CustomizeGuiOverlayEvent.BossEventProgress`, which can cancel and redraw our own bars.

## Approaches considered

- **A. Art kit first, then each screen (chosen).** A Figma style guide, then Fable draws the full kit, then the code moves to it screen by screen. This keeps everything consistent and avoids redoing screens.
- **B. Screen by screen.** Gives faster first results, but each new screen forces changes to the earlier ones.
- **C. Flagship theme first.** Gets to the polished look fastest, but the other two themes wait and drift.

## Phases (each one is a PR, reviewed by the panel, with screenshots for the owner)

1. **Style guide (Figma, owner approval gate).**
   - Per theme: palette, light and glow colour, frame family, button states, icon grid and silhouette rules, ornament set, type scale on the Minecraft font.
   - Motion rules: durations, curves, what moves.
   - Boss bar mockup.
   - Logo directions: 2–3 options.

   No code is written before the owner approves.
2. **Kit art (Fable).**
   - Textures for the three themes and the shared icon set (every skill, action, page, rank, gene, status and boss).
   - Reviewed at real size.
3. **Kit code.**
   - A texture-backed `UiKit` (nine-slice, buttons, icons, ornaments, a batched drawer) and a motion system with "reduce motion".
   - KitDraw primitives move to it.
4. **HUD on the kit.** Slots, mastery bars, mode marker, labels.
5. **Hub screens on the kit**, in order: Home, Path, Skills, Quests, Bloodline, Pack, Profile, Guide, Settings, Admin, plus dialogs (confirm, Elijah's deal).
6. **Boss bars.**
7. **Transformation animations** (Fable in Blockbench), with server timing.
8. **Logo, icon, banner.**

## Answers to the builder check (2026-11-21)

- **The Beast** is the Beast of Gévaudan from plan 17h (`feature/bosses-17h`, not yet on main when this doc was written). Its forms are Host, Beast and Spirit, and 17h syncs that state. Phases 6 and 7 cover the Beast only after 17h has merged. Klaus, Deucalion and Peter use the shared `BossForm` (HUMAN, SHIFTING, BEAST).
- **Morph timing (my call):** keep `ShiftRules.SHIFT_TICKS = 40` and `SWAP_TICK = 20` and fit each animation to them: 2 s total, with the body swap at the midpoint. The howl stays as it is, as a separate sound after the swap. The boss stays hittable throughout. If a builder finds the boss is currently invulnerable during the shift, they fix that and add a test. The existing shift tests stay valid.
- **Fable work is part of this plan.** Plan the agent briefs, the reference gathering, the in-game review at real size, and the owner check for each batch.
- **Icons (my call):** one shared icon set (32 px, plus 64 px where something is shown large). The theme sets the frame, plate and glow around each icon, not the icon itself. This keeps the asset count to about a third, and each icon reads the same in every theme.
- **Reduce motion (my call):** a new client setting `reduce_motion` (default off) on the hub Settings page, following the UI-first rule. It turns off UI tweens and flourishes in the hub, HUD and boss bars, and our screen shake. It does not change world animations such as the boss transformations or particles, because those belong to the world.
- **Layout (my call):** the style guide (phase 1) is where layout may change. The owner approves the mockups, and the layout tests are then updated to the approved geometry. Inside phases 4–5 the approved layout is fixed.
- **Themes:** the existing client `hub_theme` setting, switched live. The HUD and our boss bars always follow it, whether or not the hub is open.
- **Figma:** use the existing file `USFBf74c4I2BWDyfmRxFZO` ("SupernaturalBloodlines - Hub & HUD redesign"). The Figma MCP is available.
- **Portraits (my call):** hand-drawn pixel portraits, one per form, made from the boss skins and models as reference. They are not renders.
- `parts/hud/client/BossBars.java` reads the heights of our own boss bars, so the HUD pill stays clear of them.

## Left to the builder and the planner

- Exact texture sizes, atlas layout and file names.
- How the nine-slice is implemented (vanilla `blitSprite` with GUI sprite metadata (`gui/sprites` plus `.mcmeta` nine-slice scaling) is preferred, since 1.21.1 supports it).
- Tween easing functions.
- The order of pages inside phase 5, if dependencies suggest a better one.
- Which existing tests change, and the new kit tests.

## Open for the owner later (in the style guide review)

- Final palettes and the logo choice.
- Whether any page's layout changes beyond reskinning.

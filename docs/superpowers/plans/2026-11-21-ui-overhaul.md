# UI Overhaul Implementation Plan (hub, HUD, boss bars, transformations, logo)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the code-drawn hub and HUD with a crafted, hand-made pixel-art UI kit in three themes (Grimoire, Blood Moon, Tensura), custom boss bars, real morph animations for the multi-form bosses, and one logo, one phase and one PR at a time.

**Architecture:** Approach A from the spec: an approved Figma style guide first (phase 1), then Fable draws the whole kit (phase 2), then a texture-backed `UiKit` and a small motion system replace the `KitDraw` primitives (phase 3), then the HUD, the hub screens and the boss bars move onto the kit screen by screen (phases 4 to 6), then the transformation animations (phase 7) and the branding (phase 8). Every phase is a PR, reviewed by the panel, with screenshots at GUI scales 2, 3 and 4 in the three themes.

**Tech Stack:** Java 21, NeoForge 1.21.1, vanilla GUI sprites (`textures/gui/sprites` with `.mcmeta` nine-slice) and `GuiGraphics.blitSprite`, `CustomizeGuiOverlayEvent.BossEventProgress`, GeckoLib and Blockbench (phase 7), JUnit for the pure parts, GameTest for the world parts, Figma (phase 1 only).

**Spec:** `docs/superpowers/specs/2026-11-21-ui-overhaul-design.md`. The earlier overhaul, `docs/superpowers/plans/2026-11-16-ui-overhaul.md` (plan 16), built the current hub and HUD in code; its themes, pages, layouts and tests are the starting point and are not thrown away.

## Global Constraints

- **Pixel grid:** "Minecraft's font and GUI scale (2–4). Art must sit on a consistent pixel grid: one texel is one GUI pixel. Larger art is drawn at that same density, never scaled by fractions."
- **Contrast and access:** "text contrast stays at least 4.5:1, focus rings stay, narration stays, and nothing is cut off at scale 4." The existing tests stay green and grow to cover the kit (`Contrast`, `LabelFit`, `FocusWalk`, narration).
- **Performance:** "no per-pixel fills per frame. The kit is texture blits, batched." The performance rules from plan 16 apply.
- **Motion:** "Motion that slows play or cannot be turned off" is an anti-goal. New client setting `reduce_motion` (default off) on the hub Settings page, following the UI-first rule. It turns off UI tweens and flourishes in the hub, HUD and boss bars, and our screen shake. It does not change world animations (boss transformations, particles).
- **Art pipeline:** "Fable 5.2 agents make all art (project rule #1135)." "Gather references before drawing (#1136)." "Pixel art is drawn by hand: Blockbench texture painting, or exact pixel scripts reviewed by eye. No image-generation models." "Every asset is checked at its real size in-game before it is accepted."
- **Licences:** "our own art only. References are for inspiration and are recorded in REFERENCES.md."
- **Themes:** the existing client `hub_theme` setting, switched live. Grimoire, Blood Moon and Tensura keep their identity (book, moon, status window); "Three themes that look like one theme recoloured" is an anti-goal. The HUD and our boss bars always follow the theme, whether or not the hub is open.
- **Icons:** one shared set (16 px for HUD slots, 32 px for the hub, 64 px where large). The theme draws the frame, plate and glow around an icon, never the icon (spec decision; the 16 px companion is a phase 1 question for the owner).
- **No new mixins** unless a boss bar replacement needs one. Prefer `CustomizeGuiOverlayEvent.BossEventProgress`, which can cancel and redraw our own bars. Vanilla boss bars of other mods stay.
- **Non-goals:** new features or pages; other mods' screens (Tensura's, Enchanted's, Vampirism's, Werewolves'); vanilla bars of other mods' bosses.
- **Boss timing:** keep `ShiftRules.SHIFT_TICKS = 40` and `SWAP_TICK = 20`: 2 s total, body swap at the midpoint, the howl as a separate sound after the swap, the boss hittable throughout. The Beast of Gévaudan (Host, Beast, Spirit) is covered only after `feature/bosses-17h` has merged.
- **Process:** TDD where logic exists, server authority, Dist guards, Gradle with `--no-daemon` output to files, one report per phase. Worktree work uses absolute paths; the main folder stays clean.

## Review Focus

1. **GUI scale 4 on 1080p (480 x 270 GUI px):** a long locale string (German) in a button, chip, tab or boss-bar name must be cut by `LabelFit` with an ellipsis and a tooltip, never overflow a frame. Pinned by `LabelFitTest` cases per kit control in phase 3 and by the layout tests in phases 4 to 6.
2. **Reduce motion on:** no tween, pulse or flourish may run, every state change still lands in one frame, and the cooldown sweep (information) stays. Pinned by `ReduceMotionTest` in phase 3.
3. **A texture that is missing or replaced by a resource pack:** the kit falls back to flat theme colours, the screen stays usable, and nothing crashes. Pinned by `UiKitFallbackTest` in phase 3.
4. **Tensura not installed with `hub_theme = TENSURA`:** the Grimoire shows, with its kit, exactly as today (plan 16 behaviour). Pinned by a theme-selection test kept from plan 16 and extended in phase 3.
5. **Three or more of our bosses at once plus another mod's vanilla bar:** our bars stack with a fixed gap, never overlap the HUD pill (`parts/hud/client/BossBars` reads our bottom edge), and the other mod's vanilla bar still draws below them. Pinned by `BossBarLayoutTest` in phase 6.

---

## Phases

| Phase | PR | Gate | Detail |
|---|---|---|---|
| 1 | Style guide (Figma and PNG exports, this plan) | **Owner approval** | Full detail below |
| 2 | Kit art (Fable) | Real-size review per batch, owner check per batch | Outline below |
| 3 | Kit code (`UiKit`, motion, `reduce_motion`) | Panel review, tests green | Outline below |
| 4 | HUD on the kit | Panel review, screenshots GUI 2/3/4 x 3 themes | Outline below |
| 5 | Hub screens on the kit (Home, Path, Skills, Quests, Bloodline, Pack, Profile, Guide, Settings, Admin, dialogs) | Panel review per screen group, screenshots | Outline below |
| 6 | Boss bars | Panel review, screenshots with 1 and 3 bosses | Outline below |
| 7 | Transformation animations (Klaus, Deucalion, Peter, then the Beast) | In-game clips, GameTests | Outline below |
| 8 | Logo, mod icon, store banner | Owner picks the final files | Outline below |

**Each of phases 2 to 8 gets its own detailed plan, in `docs/superpowers/plans/`, written after phase 1 is approved** (the approved palettes, layouts and logo change what those plans say). The outlines here only fix the order, the deliverables and the gates.

---

# Phase 1: Style guide (full detail)

**Goal:** One Figma page, "UI Overhaul — Style Guide", in the file `USFBf74c4I2BWDyfmRxFZO` ("SupernaturalBloodlines - Hub & HUD redesign"), plus the same frames exported as PNGs, that the owner can approve or send back. No Java is written and Gradle is not run.

**Result (already built, see the Figma page `83:2`):** 22 frames, all pixel art made of rectangles on a grid (1 Figma px = 1 texel, shown zoomed where noted).

| Frame (Figma name) | What it shows |
|---|---|
| `00 · Read me and the pixel grid` | Grid rules, the three themes, references, owner decisions |
| `10 · Icons: grid and silhouette rules` | 16, 32 and 64 px grids, shading rules, 8 sample icons at 16 and 32 px, silhouette check |
| `11 · Motion rules` | Rules, event table (duration, easing, reduce motion), easings, three four-frame strips |
| `Kit · Grimoire`, `Kit · Blood Moon`, `Kit · Tensura` | Palette with contrast, light and glow, frame family (panel, inset, divider, scroll bar, tab, nine-slice at 1x, 2x, 4x), buttons in five states, ornaments, type scale |
| `HUD · <theme> · Supernatural mode` and `· Tensura mode` (6) | 1920 x 1080, GUI scale 2, Alt held, with a 3x detail of the slots |
| `Home · <theme>` (3), `Skills · <theme>` (3) | 1920 x 1080, GUI scale 2, the approved layouts on the new kit |
| `Boss bars · <theme>` (3) | The bars at real size (alone in the zoom, three stacked on the world) and a zoomed shift |
| `Logo · three directions` | Seal, Drop, Glyph: wordmark, emblem, 128 px mod icon, value check, lockup, mod list row |

**Files:**
- Create: `docs/superpowers/plans/2026-11-21-ui-overhaul.md` (this file)
- Create: `docs/superpowers/design/ui-overhaul/phase1/*.png` (one per frame, names below)
- Create: `docs/superpowers/design/ui-overhaul/phase1/src/*.js` (the generator that drew the page: `lib-core.js`, `kit-*.js`, `icons.js`, `mock-common.js`, `sheet-*.js`, `scene-*.js`, `RUNNER.txt`)
- Create: `docs/superpowers/design/ui-overhaul/phase1/palettes.json` (every colour of the three kits as hex, for the phase 2 briefs and the phase 3 tests)

**Interfaces:**
- Consumes: the spec; the current hub and HUD code, to name classes and keep the layouts the owner already liked (the v2 mockups, Home, Skills, Bloodline v3).
- Produces: the approved palettes (`palettes.json`), frame shapes, button states, ornament list and icon rules that phase 2 draws; the approved 1920 x 1080 layouts that phases 4 and 5 treat as fixed and that the layout tests are updated to; the chosen logo for phase 8.

### Task 1.1: Roadmap

- [x] **Step 1:** Write this file, reading the current hub and HUD code to name files and classes.
- [x] **Step 2:** Commit `docs: UI overhaul roadmap and phase 1 plan`.

### Task 1.2: Build the style guide in Figma

- [x] **Step 1:** `create_session` on the file, create the page "UI Overhaul — Style Guide".
- [x] **Step 2:** Generator library (`lib-core.js`): a pixel canvas that merges equal neighbours into rectangles, a bevel routine (light from the top-left, 1 texel darker outline), text and contrast helpers.
- [x] **Step 3:** One kit file per theme (`kit-grimoire.js`, `kit-moon.js`, `kit-tensura.js`) with the same functions: `panel`, `inset`, `divider`, `scroll`, `button(state)`, `tab`, `bar`, `focus`, `slot`, `seal`, `gem`, `plate`, `cover` or `sky`.
- [x] **Step 4:** `icons.js`: eight icons drawn from masks and shared material ramps at 16, 32 and 64 px.
- [x] **Step 5:** `sheet-kit.js` per theme; `sheet-shared.js` for the read me, icons and motion frames.
- [x] **Step 6:** `scene-hud.js`, `scene-home.js`, `scene-skills.js`, `scene-boss.js`, `scene-logo.js`.
- [x] **Step 7:** Check every frame in a screenshot, fix what looks cheap, misaligned or unreadable, repeat.

Re-run (for edits): serve the `src` folder (`python srv.py 8765 <out>`), paste `RUNNER.txt` into a Code Mode `execute`, call `sync(LIBS)` then `scene(LIBS, '<file>.js', "const THEME='grimoire';", '<note>')`. Frames are replaced by name, so a re-run does not duplicate them.

### Task 1.3: Export and commit

- [x] **Step 1:** Export every frame at 1:1 (`get_screenshot` with `maxDimension` equal to the frame width) to `docs/superpowers/design/ui-overhaul/phase1/`:
  `00-readme.png`, `10-icons.png`, `11-motion.png`, `kit-<theme>.png`, `hud-<theme>-supernatural.png`, `hud-<theme>-tensura.png`, `home-<theme>.png`, `skills-<theme>.png`, `boss-<theme>.png`, `logo.png`, with `<theme>` one of `grimoire`, `bloodmoon`, `tensura` (22 files).
- [x] **Step 2:** Open each PNG and check it: contrast, alignment, cut-off text, no mixed pixel sizes.
- [x] **Step 3:** `git add docs/superpowers` and commit `docs(design): UI overhaul phase 1 style guide (Figma exports and generator)`, then `git push -u origin feature/ui-overhaul`.

### Task 1.4: Owner review (the gate)

- [ ] **Step 1:** The owner opens the Figma page (or the PNGs) and decides: the logo direction (A Seal, B Drop, C Glyph), the final palette of each theme, the 16 px companion icon set for the HUD, the custom 16 px title bitmap font, and whether any page layout changes beyond reskinning.
- [ ] **Step 2:** Record the answers in `docs/superpowers/design/ui-overhaul/phase1/APPROVED.md`: the chosen logo, any colour that changed (hex), the approved layouts (frame names), and the list of open items. Update the Figma frames if the owner asked for changes and re-export.
- [ ] **Step 3:** Write the detailed plan of phase 2 (and, from then on, of each next phase) before it starts. No Java is written before this step is done.

---

# Phase 2: Kit art (Fable)

**Goal:** Every texture the kit needs, drawn by hand, accepted at real size in the game.

**Deliverables:**
- **Frames and controls per theme (x3):** panel, inset, divider, scroll bar track and thumb, tab (normal, selected, hover, disabled), buttons (primary and secondary; normal, hover, pressed, disabled), progress bar, focus ring, slot frames (square, round, hex), seal and gem ornaments, key badges, name plates, boss portrait plates and form chips. Each is a GUI sprite with a `.mcmeta` nine-slice where it scales.
- **Shared icons:** 16 px (HUD, lists), 32 px (hub), 64 px (large) for every skill, action, page, rank, gene, status and boss (the 36 icons of `icons.png` become the first list; the full manifest comes from `SkillIcons`, `Icons` and the skill registries).
- **Boss portraits:** one 24 x 24 per form for Klaus, Deucalion and Peter (`BossForm` HUMAN, SHIFTING, BEAST) and later the Beast (Host, Beast, Spirit), hand-drawn from the boss skins and models.
- **Title bitmap font** (16 px, a `font/*.json` bitmap provider) if the owner approves it.
- `REFERENCES.md`: what was looked at, and what was taken.

**Gate:** Fable agents work in batches (frames, controls, ornaments, icons, portraits, font). Each batch starts with reference gathering (#1136), is drawn from the approved style guide and `palettes.json`, is checked at real size at GUI scales 2, 3 and 4 in the game, and is shown to the owner before the next batch. Agent briefs, the review checklist and the batch order are written in the phase 2 plan.

**Main files:** `src/main/resources/assets/supernaturalbloodlines/textures/gui/**` (existing: `icons.png`, `icons.txt`, `hub/grimoire/parchment.png`, `hub/grimoire/widgets.png`, `hud/*.png`), new `textures/gui/sprites/<theme>/**`, `textures/gui/icons/{16,32,64}/**`, `textures/gui/boss/**`, `font/**`; retire `tools/gen_icons.py` when the atlas is replaced; a texture manifest test (every listed icon exists at all sizes, sizes and nine-slice values are valid, only palette colours are used).

# Phase 3: Kit code

**Goal:** A texture-backed `UiKit` that draws the kit (nine-slice, buttons, icons, ornaments, a batched drawer), a motion system, and the `reduce_motion` setting, with `KitDraw` primitives moved onto it.

**Deliverables:** `UiKit` (blit a sprite, nine-slice panel, button states, icon by name and size, ornament, flat-colour fallback for a missing texture); `Motion` and `Easing` (pure, eased tweens on the client tick for offset in whole texels, alpha and frame steps; a cursor object per target); `reduce_motion` in `HubClientConfig`, on the Settings page, honoured by `Motion`; `KitDraw` methods (`shape`, `medallion`, `button`, `dashed`, `focusRing`, `tickRing`, `pips`, `emblem`, `counters`, `detail`, `confirm`, `dialogFrame`, `dialogButton`) delegate to `UiKit`; `HubTheme` default methods (`drawFrame`, `drawTab`, `drawSeal`, `drawStamp`, `drawBar`, `drawScrollbar`, `drawFocus`, `drawThemeButton`, `drawClose`) get kit-based defaults.

**Gate:** all existing tests green (`ContrastTest`, `ThemeContrastTest`, `LabelFitTest`, `FocusOrderTest`, `FocusWalkTest`, `NarrationTest`, the layout tests) plus new `EasingTest`, `MotionStateTest`, `ReduceMotionTest`, `UiKitSlicesTest`, `UiKitFallbackTest`; panel review.

**Main files:** `shared/hub/kit/client/KitDraw.java`, `KitClient.java`, `BakedTexture.java`, `UiContext.java`; `shared/hub/kit/Bake.java`, `Icons.java`, `FocusRing.java`; `shared/hub/HubTheme.java`; new `shared/hub/kit/client/UiKit.java`, `shared/hub/kit/Motion.java`, `Easing.java`; `parts/supernatural/client/hub/HubClientConfig.java`, `page/SettingsPage.java`, `HubClient.java`, `parts/hud/client/HudClientConfig.java`.

# Phase 4: HUD on the kit

**Goal:** The HUD drawn only from kit sprites, in the approved layout of the style guide.

**Deliverables:** slot frames in the theme shape (Grimoire inked tile, Blood Moon round, Tensura-mode square with the teal mark), 16 px icons, key badges, cooldown sweep, active and ready glow, the blood ring on Bite, mastery bars, mode pill, Alt loadout panels, status card and moon clock; motion for the press, the ready glow and the flourishes; screenshots at GUI scales 2, 3 and 4 in the three themes and both modes.

**Gate:** `SlotArtShapeTest`, `HudPanelsTest`, `HudContrastTest`, `HudModeStateTest`, `HudInputRulesTest` stay green, new tests for the kit-drawn slots; panel review.

**Main files:** `parts/hud/client/SlotArt.java`, `HudSlots.java`, `HudChrome.java`, `AbilityHudLayer.java`, `LiveHud.java`, `HudSource.java`, `HudStanding.java`, `HudDemo.java`, `tensura/TensuraSlots.java`, `shared/hud/HudStyle.java`, `ClientIcons.java`; `core/hud/SlotShape.java`, `SlotView.java`, `PieSweep.java`, `SlotBadge.java` (logic stays).

# Phase 5: Hub screens on the kit

**Goal:** Every hub page on the kit, one page group at a time, in this order unless a dependency says otherwise: Home, Path, Skills, Quests, Bloodline, Pack, Profile, Guide, Settings, Admin, then the dialogs (confirm, Elijah's deal). Rites and Loadout follow with their page group.

**Deliverables per screen:** the three theme views on the kit, the approved layout from the style guide (the layout tests are updated to the approved geometry in the same PR), page-turn and press motion, empty and long-text states, screenshots at GUI scales 2, 3 and 4 in the three themes.

**Gate:** `ThemeContrastTest`, `NewPagesContrastTest`, `GhoulMoonContrastTest`, `FocusWalkTest`, `NarrationTest`, `LabelFitTest`, `HubLangCoverageTest`, `HubPagesTest`, `PageRowsLayoutTest`, `DetailLayoutTest`, `ChoiceLayoutTest` green; panel review per group.

**Main files:** `parts/supernatural/client/hub/HubScreen.java`, `HubPages.java`, `HubThemeViews.java`, `HubView.java`, `theme/GrimoireTheme.java` + `GrimoireLayout.java`, `theme/BloodMoonTheme.java` + `BloodMoonLayout.java`, `parts/supernaturaltensura/client/TensuraHubTheme.java` + `TensuraHubLayout.java`; pages in `parts/supernatural/client/hub/page/` (`HomePage`, `PathPage`, `BloodMoonPathPage`, `StagePathPage`, `SkillsPage`, `BookSkillsPage`, `QuestsPage`, `BookQuestsPage`, `StageQuestsPage`, `TaskCard`, `BloodlinePage`, `GrimoireBloodlinePage`, `StageBloodlinePage`, `PackPage`, `BookPackPage`, `ProfilePage`, `GuidePage`, `BookGuidePage`, `StageGuidePage`, `SettingsPage`, `AdminPage`, `RitesPage`, `LoadoutPage`, `PageKit`, `PageWidgets`, `EmptyState`); `shared/hub/kit/ConfirmLayout.java`, `ChoiceLayout.java`, `DetailLayout.java`, `Detail.java`.

# Phase 6: Boss bars

**Goal:** Our own boss bars (themed frame, portrait plate per form, name plate, form indicator, health with ticks) replacing vanilla's bar for our bosses only.

**Deliverables:** a client renderer on `CustomizeGuiOverlayEvent.BossEventProgress` that cancels our bosses' vanilla bar and draws ours (no mixin unless that proves impossible); a pure `BossBarLayout` (stack positions, gap, bottom edge); the form carried from the server (`BossForm` for Klaus, Deucalion and Peter; Host, Beast, Spirit for the Beast after `feature/bosses-17h` merges) in the bar update; health easing and the chip swap with `reduce_motion`; `parts/hud/client/BossBars` updated to read our bars' bottom edge so the HUD pill stays clear; screenshots with 1 and 3 bosses in the three themes.

**Gate:** `BossBarLayoutTest` (stacking, gap, bottom edge, a vanilla bar of another mod below ours), `BossBarFormTest`, contrast of the chips and names; panel review.

**Main files:** `parts/bosses/director/BossBars.java` (server, `ServerBossEvent`), new `parts/bosses/client/BossBarView.java`, `BossBarLayout.java`, `BossPortraits.java`, a form sync payload next to `parts/bosses/entity/ShiftCore.java`, `core/boss/BossForm` and `ShiftRules`, `parts/hud/client/BossBars.java`, `parts/species/pack/AlphaTrialBoss.java` (its own bar stays vanilla unless the owner asks).

# Phase 7: Transformation animations

**Goal:** Real morph animations for Klaus, Deucalion, Peter and (after 17h) the Beast: GeckoLib sequences made in Blockbench by Fable, 1.5 to 2.5 s long (the shift is 2 s), the boss vulnerable throughout, with particles and sound.

**Deliverables:** the animation files and any model changes; the animation fitted to `SHIFT_TICKS = 40` with the body swap at `SWAP_TICK = 20` and the howl as a separate sound after the swap; particles and sound per boss; a check that the boss is hittable during the shift, and a fix with a test if it is invulnerable today; an in-game clip per boss.

**Gate:** the existing shift tests stay valid; new tests for the hit window; a reviewed in-game clip per boss; panel review.

**Main files:** `core/boss/ShiftRules.java`; `parts/bosses/entity/ShiftCore.java`, `ShiftBody.java`, `KlausEntity.java`, `DeucalionEntity.java`, `PeterHaleEntity.java`, `DeucalionEvents.java`, `PeterEvents.java`; `parts/bosses/client/ShiftRenderer.java`, `HumanoidBossRenderer.java`, `BossRenderers.java`, `BossShots.java`; `assets/supernaturalbloodlines/geckolib/**` and `animations/**`; `parts/bosses/gametest/KlausGameTests.java`, `DeucalionGameTests.java`, `PeterGameTests.java`.

# Phase 8: Logo, icon, banner

**Goal:** The chosen logo as the mod icon, the mod-list logo and the store banner, one consistent mark.

**Deliverables:** `logo.png` (128 x 128, 2x of the 64 px tile), the 64 px original, a wordmark file, the store banner and the README header; `neoforge.mods.toml` `logoFile`; the asset files kept as sources.

**Gate:** the owner picks the final files; the icon is checked in the mod list at the real size.

**Main files:** `src/main/resources/META-INF/neoforge.mods.toml`, `src/main/resources/logo.png`, `README.md`, `docs/`.

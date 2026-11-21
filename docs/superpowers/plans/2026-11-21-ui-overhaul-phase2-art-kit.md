# UI Overhaul Phase 2: Art Kit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [x]`) syntax for tracking.

**Goal:** Every texture the UI overhaul needs, as hand-made pixel-art PNGs: one kit per theme (Grimoire, Blood Moon, Tensura), one shared icon set at 32 and 16 px, 15 boss portraits, the title font atlas and the logo. No Java is wired and Gradle is not run (that is phase 3).

**Architecture:** One deterministic generator in Python with Pillow (`docs/superpowers/design/ui-overhaul/phase2/src/`), a port of the phase 1 generator's pixel canvas: masks, a bevel routine lit from the top-left with a 1 texel outline, shared material ramps and bitmaps typed row by row. Every pixel comes from `palettes.json` or a named ramp. The generator writes the PNGs into `src/main/resources`, a `manifest.json` (name, size, nine-slice borders, use) and the contact sheets. Nothing is scaled by a fraction and no image model is used.

**Tech Stack:** Python 3.14 (`C:\Users\caiom\AppData\Local\Python\pythoncore-3.14-64\python.exe`), Pillow 12, PNG (RGBA, 8 bit).

**Spec:** `docs/superpowers/specs/2026-11-21-ui-overhaul-design.md`. **Roadmap:** `docs/superpowers/plans/2026-11-21-ui-overhaul.md`. **Style guide:** `docs/superpowers/design/ui-overhaul/phase1/` and the Figma page `83:2`.

## Owner decisions (phase 1 gate)

- **Logo:** direction B, "Drop": a blood drop with the moon cut in, in a red corona.
- **HUD icons:** hand-drawn 16 px icons for the HUD slots; 32 px icons are for the hub.
- **Fonts:** a custom 16 px bitmap font for titles only; body text stays in the Minecraft font.
- **Layouts:** the approved page layouts stay; this is a reskin.
- **Palettes:** as drawn in `phase1/palettes.json`.

## Global Constraints

- One texel is one GUI pixel. No anti-aliasing, no smooth gradients, no mixed pixel sizes. Glow is stepped alpha (100 / 60 / 32 / 16 / 8 %), never a blur.
- Light from the top-left; the outline is 1 texel, a darker shade of the shape's own colour.
- Icons are shared by the three themes. The theme draws the frame, plate and glow, never the icon.
- Text on a kit surface keeps a contrast of at least 4.5:1 (the pairs of the style guide).
- Our own art only. References: the phase 1 style guide and the boss skins in `textures/entity/boss/` (colours and features only; portraits are drawn, not rendered).
- Paths below are relative to `src/main/resources/assets/supernaturalbloodlines/textures/gui/`.
- Kit sprites and icons sit in `textures/gui/sprites/` (owner's answer after the first review), so phase 3 draws them with `blitSprite`. A sprite that scales has a `.mcmeta` next to it (`nine_slice` with its border, or `tile`); `phase2/manifest.json` lists the same values and each sprite id.

## What the code draws today (the source of the inventory)

| Code | Draws | Kit sprite that replaces it |
|---|---|---|
| `HubTheme.drawFrame`, `GrimoireTheme`, `BloodMoonTheme`, `TensuraHubTheme` | book cover, sky and moon, status window | `cover`, `moon_large`, `titlebar`, `panel`, `inset` |
| `HubTheme.drawTab`, `drawGroupHeading` | page tabs, group headings | `tab*`, `divider`, page icons |
| `HubTheme.drawSeal`, `drawLauncher` | the round seals and launcher tiles (E seal, other mods) | `seal*` and an icon |
| `HubTheme.drawRungMark` (`RungStatus`) | reached, current, locked | `rung_*` |
| `HubTheme.drawTaskBox` (`TaskState`) | claimable, open, claimed, frozen, unavailable | `task_*` |
| `HubTheme.drawStamp` | the claimed stamp | `stamp`, `stamp_lit` |
| `HubTheme.drawRule`, `drawBar`, `drawScrollbar`, `drawFocus` | rule, progress bar, scroll bar, focus ring | `divider`, `divider_gem`, `bar_*`, `scroll_*`, `focus` |
| `HubTheme.drawThemeButton`, `drawClose`, `KitDraw.button`, `dialogButton`, `detailActions` | buttons | `button_*`, `button_primary_*` and an icon |
| `KitDraw.medallion`, `shape`, `emblem` | skill nodes, emblem | `node_*`, 32 px icons |
| `KitDraw.pips`, `tickRing`, `counters` | pips and counters | `pip_*`, `plate` |
| `KitDraw.sourceTag` (`SourceTag` SB, VMP, WW, ENC, TEN) | source tags | `chip` and `tag_*` icons |
| `KitDraw.dialogFrame`, `confirm`, `detail` | dialogs, detail card, tooltips | `panel`, `tooltip`, `corner_*`, `flourish_*` |
| `Icons` (45 cells of `icons.png`), `SkillIcons` (42 skills on 13 cells) | every hub icon | `icons/32/*`, `icons/16/*` |
| `SlotArt` | slot base per theme and per `SlotShape`, key badge, cooldown pie, lock, blood ring, Tensura mark | `slot*`, `key_badge*`, `shared/cooldown_sweep`, `shared/blood_ring`, `shared/mode_mark`, `icons/16/lock` |
| `HudChrome`, `AbilityHudLayer` | status card, mode pill, Alt labels, moon clock, mastery bar | `panel`, `face_plate`, `hud_pill`, `hud_label`, `mastery_*`, moon icons |
| `ClientLoadout.BITE_ICON`, `AlphaRoarAction`, `ClientIcons.UNKNOWN`, `BloodInFoodSpot` | our own HUD icons | `icons/16/bite`, `alpha_roar`, `unknown`, `blood_full`, `blood_empty` |
| `parts/bosses/director/BossBars` (vanilla bar today) | boss bars | `boss_*`, `chip*`, `boss/*` portraits |

Icons of Vampirism, Werewolves and Tensura actions stay those mods' own textures (`ClientIcons` draws them at 16 px inside our slot frame).

## Inventory

### A. Theme kit: `sprites/kit/<theme>/`, with `<theme>` one of `grimoire`, `bloodmoon`, `tensura` (the same 85 names in each)

"9s" is a nine-slice with that border; "3s" is a three-slice along one axis. A canvas larger than the body leaves room for the glow or the focus ring, so every state of a control has the same canvas. The exact canvas and slice of every sprite are in `phase2/manifest.json`, which the generator writes.

| Sprite | Canvas | Slice | Used for |
|---|---|---|---|
| `panel` | 32 x 32 | 9s 6 to 8 (Blood Moon: 17 on the left, for the blood line) | page panels, cards, the status card |
| `panel_texture` | 64 x 64 | tile | laid over a panel or the cover: paper fibres, stars, scanlines |
| `inset` | 16 x 16 | 9s 3 | wells, list rows, text fields |
| `tooltip` | 24 x 24 | 9s 5 | tooltips and the detail popover |
| `divider` | 16 x 7 | 3s 4 | rules (`drawRule`) |
| `divider_gem` | 15 x 9 | none | the ornament at the middle of a rule |
| `scroll_track` | 7 x 16 | 3s 3 (vertical) | scroll bar track |
| `scroll_thumb`, `scroll_thumb_hover` | 7 x 14 | 3s 4 to 5 (vertical) | scroll bar thumb |
| `tab`, `tab_hover`, `tab_selected`, `tab_disabled` | 32 x 16 | 9s | page tabs (book index tab, moon pill, window list row) |
| `button_normal`, `_hover`, `_pressed`, `_disabled`, `_focus` | 28 x 20, body 24 x 16 | 9s 6 (Blood Moon: 3s 10) | secondary buttons |
| `button_primary_normal`, `_hover`, `_pressed`, `_disabled`, `_focus` | 28 x 20, body 24 x 16 | 9s 6 (Blood Moon: 3s 10) | primary buttons |
| `focus` | 12 x 12 | 9s 3 | the 2 px focus ring on anything else |
| `bar_track`, `bar_fill` | 16 x 8, 8 x 4 (Tensura fill: a 6 x 4 tile) | 9s, 3s | progress bars (`drawBar`) |
| `plate` | 24 x 14 | 9s | name plates, counters |
| `chip`, `chip_on`, `chip_shifting` | 16 x 12, body 14 x 10 | 9s | rank and source tags, boss form chips |
| `seal`, `seal_hover`, `seal_pressed` | 20 x 20 | none | rail seals and launcher tiles |
| `gem_red`, `gem_blue`, `gem_gold`, `gem_green`, `gem_violet` | 11 x 11 | none | gems on plates, tags and the mode pill |
| `corner_tl`, `_tr`, `_bl`, `_br` | 12 x 12 | none | corner fittings of dialogs and the cover |
| `flourish_tl`, `_tr`, `_bl`, `_br` | 11 x 11 | none | ink filigree in page corners |
| `flourish_title` | 48 x 7 | none | the flourish under a page title |
| `stamp`, `stamp_lit` | 16 x 16 | none | `drawStamp` |
| `rung_reached`, `rung_current`, `rung_locked` | 12 x 12 | none | `drawRungMark` |
| `task_claimable`, `task_open`, `task_claimed`, `task_frozen`, `task_unavailable` | 10 x 10 | none | `drawTaskBox` |
| `pip_on`, `pip_off` | 5 x 5 | none | `KitDraw.pips` |
| `node_off`, `node_on`, `node_selected` | 34 x 34, body 26 | none | skill nodes and medallions (round, round, hex) |
| `cover` | Grimoire 50 x 50 (9s 13), Blood Moon 12 x 272 (tiles sideways, the last band fills the rest), Tensura 32 x 32 (9s 10) | see left | the hub background |
| `cover_art` | Grimoire 32 x 32 brass boss, Blood Moon 64 x 64 moon in its corona, Tensura 24 x 13 title bar (the left 12 fixed, the rest tiles) | none | the theme's signature piece |
| `slot`, `slot_active` | 28 x 28, body 22 | none | HUD slot in the theme's shape (tile, round, hex) |
| `slot_square`, `slot_square_active` | 28 x 28, body 22 | none | HUD slot in Tensura mode |
| `key_badge`, `key_badge_wide` | 11 x 11, 17 x 11 | none | the key badge (wax seal, plate, tab) |
| `hud_label` | 16 x 16 | 9s 3 | the Alt label box |
| `hud_pill` | 24 x 14 | 9s | the mode pill |
| `face_plate` | 20 x 20 | none | the frame of the player's face on the status card |
| `mastery_track`, `mastery_fill`, `mastery_fill_learning`, `mastery_fill_mastered` | 18 x 4, 2 x 2 | 3s, tile | `MasteryBar` |
| `boss_frame` | 32 x 32 | 9s 6 | the boss bar card (250 x 46) |
| `boss_portrait_plate`, `boss_portrait_plate_shifting` | 42 x 42, body 34, window 24 | none | drawn over the 24 x 24 portrait |
| `boss_name_plate` | 24 x 13 | 9s | the boss name |
| `boss_bar_track` | 18 x 11, body 16 x 9 | 9s | the health bar |
| `boss_bar_fill`, `boss_bar_fill_shifting` | 8 x 7 | tile | health, and the gold striped fill of a shift |
| `boss_bar_tick` | 1 x 7 | none | ticks at 25, 50 and 75 % |

### B. Shared HUD pieces: `sprites/kit/shared/`

| Sprite | Canvas | Used for |
|---|---|---|
| `cooldown_sweep` | 16 x 528 (33 frames of 16 x 16 down the file, frame 0 empty) | the clockwise cooldown pie (`PieSweep`) |
| `blood_ring` | 32 x 800 (25 frames of 32 x 32 down the file) | the ring of blood round Bite |
| `mode_mark` | 7 x 7 | the corner diamond of Tensura mode |
| `blood_full`, `blood_empty` | 9 x 9 | the blood points `BloodInFoodSpot` draws over the food bar |

### C. Icons: `sprites/icons/32/<name>.png` (hub) and `sprites/icons/16/<name>.png` (HUD and lists), the same names in both

- **Pages (11):** `home`, `path`, `skills`, `quests`, `rites`, `bloodline`, `pack`, `guide`, `loadout`, `settings`, `admin`. (`rites` stays for the E seal that opens Enchanted's book; the page itself is gone.)
- **Controls and status (15):** `check`, `lock`, `crown`, `dot`, `close`, `theme`, `launch`, `question`, `info`, `plus`, `arrow_left`, `arrow_right`, `bell`, `key`, `unknown`.
- **Sky (9):** `moon_new`, `moon_waxing_crescent`, `moon_first_quarter`, `moon_waxing_gibbous`, `moon_full`, `moon_waning_gibbous`, `moon_last_quarter`, `moon_waning_crescent`, `sun`.
- **Motifs (11, the fallbacks of `SkillIcons`):** `drop`, `shield`, `fang`, `vial`, `claw`, `heart`, `boot`, `eye`, `voice`, `spark`, `bolt`.
- **HUD actions (6):** `bite`, `alpha_roar`, `thirst`, `moonblood_surge`, `wolf_spirits`, `leap`. The lock of a locked slot is `lock`, the missing icon is `unknown`.
- **Skills (42), one per skill id in `data/.../supernatural/skill/`:**

| Id | Skill | Picture |
|---|---|---|
| `s-hum-1` | Survivor | a heart with a bandage |
| `s-hum-2` | Vervain Blood | a blood drop with a vervain sprig |
| `s-hum-3` | Swift Feet | a boot with speed lines |
| `s-hun-1` | Steady Aim | a crosshair |
| `s-hun-2` | Hunter's Eye | an eye with a crosshair pupil |
| `s-hun-3` | Patient Hunter | an hourglass |
| `s-hun-4` | Danger Sense | a warning triangle |
| `s-hun-5` | Mark Mastery | a target with an arrow in it |
| `s-hyb-1` | Steady Thirst | a half-full blood drop |
| `s-hyb-2` | Venom Fang | a fang with a green drop |
| `s-ori-1` | Old Blood | a crowned blood drop |
| `s-ori-2` | Bloodmaker's Yield | a full vial with a drop above |
| `s-ori-3` | Sire's Call | a horn with red sound waves |
| `s-ori-4` | Primordial Mind | an eye in a violet ring |
| `s-sip-1` | Deep Draw | a violet spiral |
| `s-sip-2` | Quick Draw | a spiral with a bolt |
| `s-sip-3` | Life Siphon | a heart with a violet spiral |
| `s-sip-4` | Spent Wisely | gold coins |
| `s-sip-5` | Void Hunger | a dark maw in a violet ring |
| `s-vam-1` | Keen Fangs | two sharp fangs with a glint |
| `s-vam-2` | Night Stride | a boot under a crescent |
| `s-vam-3` | Silver Tongue | a silver speech mark |
| `s-vam-4` | Hardened | a steel shield |
| `s-vam-5` | Blood Mist | a red cloud that rains blood |
| `s-vam-6` | Predator's Thirst | claw marks with a drop |
| `s-vam-7` | Elder's Skin | a shield in front of the sun |
| `s-wit-1` | Steady Circle | a chalk circle with a centre dot |
| `s-wit-2` | Wide Circle | a double circle |
| `s-wit-3` | Ashwarden | a shield with an ash leaf |
| `s-wit-4` | Emissary's Bond | two linked rings |
| `s-wit-5` | Magic Sense | a violet eye with a spark |
| `s-wit-6` | Ancestral Voice | a candle flame with sound waves |
| `s-wit-7` | Coven's Ward | a violet shield with a star |
| `s-wol-1` | Thick Hide | a fur shield with a scratch |
| `s-wol-2` | Moon Calm | a full moon over still water |
| `s-wol-3` | Godwolf Sense | a gold wolf eye |
| `s-wol-4` | Wolfsbane Tolerance | a wolfsbane flower |
| `s-wol-5` | Pack Heart | a heart with a paw print |
| `s-wol-6` | Roar's Echo | doubled sound waves |
| `s-wol-7` | Moon Heart | a heart on a full moon |
| `s-wol-8` | Primeval Roar | a roaring wolf head |
| `s-wol-11` | Demon Wolf Roar | a dark wolf head with red eyes |

- **Bloodlines (12, one per ladder):** `species_human`, `species_hunter`, `species_vampire`, `species_werewolf`, `species_witch`, `species_hybrid`, `species_original`, `species_original_hybrid`, `species_siphoner`, `species_heretic`, `species_tribrid`, `species_ghoul`.
- **Pack ranks (4):** `rank_omega`, `rank_beta`, `rank_alpha`, `rank_true_alpha`.
- **Source tags (5):** `tag_sb`, `tag_vmp`, `tag_ww`, `tag_enc`, `tag_ten`.

That is 115 icons at two sizes, 230 files. The 64 px size of the roadmap is left to the phase that first shows an icon that large.

### D. Boss portraits: `boss/<name>.png`, 24 x 24 each (15)

`mikael`, `klaus_human`, `klaus_shifting`, `klaus_wolf`, `elijah`, `rebekah`, `kol`, `finn`, `deucalion_human`, `deucalion_demon_wolf`, `peter_human`, `peter_wolf`, `beast_host`, `beast_beast`, `beast_spirit`.

Faces are drawn from the skins: hair, skin and eye colours, beard, glasses, clothes at the collar. Wolves use the fur and eye colours of the geo textures. The three dark wolves differ in shape, following `art/bosses/README.md`: Deucalion's demon wolf has blue-grey skin, swept-back hair, a ridged brow and red eyes; Peter's wolf is black with a mane, the red glare of an Alpha and a scar through one eye; the Beast has russet streaks, bone-white brow band, skull cap, cheek guards and muzzle ridge, and white-blue eyes.

### E. Title fonts: `textures/font/title.png`, `textures/font/title_gothic.png` and their `font/*.json`

A 16 px line with a cap height of 11, an x-height of 8 and stems 2 texels wide. Cells of 12 x 16 in a 16-column atlas, 173 glyphs: ASCII 33 to 126, the Latin-1 letters (À to ÿ) with the accents composed over the base letters, and the typographic marks (dashes, curly quotes, ellipsis, bullet). The JSON has a `space` provider (5 px) and a `bitmap` provider (`height` 16, `ascent` 14); Minecraft trims each glyph's width itself.

There are two fonts with the same metrics and the same 173 glyphs. `title` is the plain one, for Tensura. `title_gothic` is for Grimoire and Blood Moon: thick stems, hairline bars, pointed bowls, a flag on the head of each stem and a foot at its base, a hairline inside the round capitals. It keeps roman letter shapes so it reads at real size; punctuation and accents are shared with the plain font.

### F. Logo (direction B, Drop)

| File | Size | Used for |
|---|---|---|
| `src/main/resources/logo.png` | 64 x 64 | the mod icon (`logoFile` in `neoforge.mods.toml`, wired in phase 8) |
| `sprites/kit/shared/logo_128.png` | 128 x 128 | the 64 px tile at an exact 2x, for stores |
| `sprites/kit/shared/logo_emblem.png` | 56 x 56 | the emblem alone, for the hub home page |
| `sprites/kit/shared/logo_wordmark.png` | 131 x 34 | "SUPERNATURAL BLOODLINES" |
| `sprites/kit/shared/logo_lockup.png` | 193 x 56 | emblem and wordmark together |

---

## Tasks

### Task 1: Plan and generator core

**Files:**
- Create: `docs/superpowers/plans/2026-11-21-ui-overhaul-phase2-art-kit.md` (this file)
- Create: `docs/superpowers/design/ui-overhaul/phase2/src/px.py` (canvas, masks, bevel, ramps, PNG output, contact sheets)

- [x] **Step 1:** Write this plan from the current hub and HUD code.
- [x] **Step 2:** Port the phase 1 canvas (`lib-core.js`, `kit-common.js`) to `px.py`: `Cv` with `px`, `rect`, `box`, `line`, `disc`, `ring`, `poly`, `bmp`, `blit`, `outline`, `bevel`; masks `rr`, `notch`, `pill`, `disc_mask`, `hex_mask`; `save` and `sheet`.
- [x] **Step 3:** Commit `docs: UI overhaul phase 2 plan (art kit inventory)`.

### Task 2: Theme kits

**Files:**
- Create: `phase2/src/kit.py`
- Create: `textures/gui/sprites/kit/{grimoire,bloodmoon,tensura}/*.png`, `textures/gui/sprites/kit/shared/*.png`, and a `.png.mcmeta` for each sprite that scales
- Create: `phase2/kit-{grimoire,bloodmoon,tensura}.png`, `phase2/kit-shared.png` (contact sheets)

- [x] **Step 1:** Port the three kit recipes of phase 1 and draw every sprite of table A, then table B.
- [x] **Step 2:** Run `python build.py kit`. Expected: 85 files per theme, 5 shared, and the contact sheets.
- [x] **Step 3:** Open each contact sheet (real size and 4x). Fix anything blurry, misaligned or low in contrast, and anything that makes two themes look like recolours of each other.

### Task 3: Icons

**Files:**
- Create: `phase2/src/icons.py`
- Create: `textures/gui/sprites/icons/32/*.png`, `textures/gui/sprites/icons/16/*.png`
- Create: `phase2/icons-32.png`, `phase2/icons-16.png` (each on a light and a dark ground, with the silhouette row)

- [x] **Step 1:** Draw the motifs and the 115 icons of list C on the 32 grid, each with its own 16 px drawing where the 32 px one does not read.
- [x] **Step 2:** Run `python build.py icons`. Expected: 230 files, two contact sheets and two silhouette sheets.
- [x] **Step 3:** Review at real size on light and dark: each silhouette reads alone, no icon touches the canvas edge, no two skills look the same.

### Task 4: Boss portraits

**Files:**
- Create: `phase2/src/portraits.py`
- Create: `textures/gui/boss/*.png` (15)
- Create: `phase2/portraits.png`, `phase2/portraits-wolves.png` (the three dark wolves at 8x)

- [x] **Step 1:** Draw the 15 portraits of list D from the skin colours.
- [x] **Step 2:** Review at real size inside each theme's portrait plate.

### Task 5: Title font

**Files:**
- Create: `phase2/src/font.py`
- Create: `textures/font/title.png`, `textures/font/title_gothic.png`, `assets/supernaturalbloodlines/font/title.json`, `font/title_gothic.json`
- Create: `phase2/font-title.png` (the atlas and sample lines in English, German, Portuguese, French and Spanish), `phase2/font-title-gothic.png`, `phase2/font-title-themes.png`

- [x] **Step 1:** Draw the glyphs and compose the accents.
- [x] **Step 2:** Review the samples: even stem weight, even spacing, accents clear of the line above.

### Task 6: Logo

**Files:**
- Create: `phase2/src/logo.py`
- Create: `src/main/resources/logo.png`, `textures/gui/sprites/kit/shared/logo_*.png`
- Create: `phase2/logo.png` (contact sheet with the greyscale value check and the mod list row), `phase2/logo-128.png`

- [x] **Step 1:** Draw the Drop emblem, the tile, the wordmark and the lockup.
- [x] **Step 2:** Review at 64 px, at 128 px and in greyscale.

### Task 7: Manifest, self-check, commit

**Files:**
- Create: `phase2/manifest.json`, `phase2/REFERENCES.md`, `phase2/src/build.py`

- [x] **Step 1:** `build.py` asserts: every name of this plan exists at its size, the three themes have the same file names, every icon exists at 16 and 32 px, no icon touches its canvas edge, and no sprite has a partly transparent pixel outside the listed glow sprites.
- [x] **Step 2:** `git status --short` in the main folder is empty.
- [x] **Step 3:** Commit `feat(art): UI overhaul phase 2 art kit` and push `feature/ui-overhaul`.

## Review checklist (every contact sheet)

1. Real size first: does it read in one glance at GUI scale 2?
2. One light direction, 1 texel outlines, no stray pixels, no half-transparent edges.
3. Text colours of the style guide still pass 4.5:1 on the surface.
4. The three themes differ in shape and material, not only in colour: book and brass, moon and pills, window and notches.
5. Nothing looks generated: no soft gradients, no noise that has no reason.

## Result

512 PNG files: 255 theme sprites (85 x 3), 9 shared (5 HUD pieces and 4 logo files), 230 icons, 15 portraits, 2 font atlases and `logo.png`; plus 124 `.mcmeta` files and 2 font JSON files. Re-run with `python docs/superpowers/design/ui-overhaul/phase2/src/build.py all`; the run ends with the self-check of task 7.

Not done here, by the brief: no Java is wired (`Icons`, `SkillIcons`, `SlotArt`, `neoforge.mods.toml` are untouched) and nothing was checked in the running game. The real-size check in game at GUI scales 2, 3 and 4 belongs to the phase that first draws each sprite.

## Owner's answers (review of the kit)

- Skills keep one icon each (42).
- The sprites live in `textures/gui/sprites/` with `.mcmeta` scaling (done).
- A second, gothic title font for Grimoire and Blood Moon; the plain one stays for Tensura (done).
- The store banner (phase 8) uses the lockup as drawn.
- Polish done: the 16 px `leap`, `rites` and `s-sip-3` redrawn; the three dark wolf portraits redrawn.

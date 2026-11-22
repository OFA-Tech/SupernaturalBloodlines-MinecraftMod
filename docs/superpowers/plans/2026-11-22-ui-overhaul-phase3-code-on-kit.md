# UI Overhaul Phase 3: Code on the Kit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** The shared drawing layer draws the phase 2 art kit (`blitSprite` of the sprites in `textures/gui/sprites/`) instead of hand-drawn fills, titles use the two bitmap fonts, icons come from the new 16 and 32 px sprites by name (one per skill), and a small motion system with a `reduce_motion` setting exists. Page-by-page restyling of the HUD, the hub screens and the boss bars is phases 4 to 6 and is **not** built here.

**Architecture:** Pure rules first (easing, durations, a per-element animation clock, the reduce-motion switch, sprite names, the icon-fit rule, the bar, scroll bar and mark maths, the title-font glyph list), each with a unit test that fails when the rule is reverted. A thin client layer on top (`UiKit`, `KitWidgets`, `UiMotion`) does the `blitSprite` calls; a sprite that is missing (a resource pack) is drawn flat in the theme's colours. `KitDraw` and the three theme classes call that layer; their layouts and the pages are untouched.

**Tech Stack:** Java 21, NeoForge 1.21.1, `GuiGraphics.blitSprite` with GUI sprite `.mcmeta` (nine-slice, tile), JUnit 5, hidden `runSafeShots`.

**Spec:** `docs/superpowers/specs/2026-11-21-ui-overhaul-design.md`. **Roadmap:** `docs/superpowers/plans/2026-11-21-ui-overhaul.md` (phase 3 outline). **Art:** `docs/superpowers/plans/2026-11-21-ui-overhaul-phase2-art-kit.md`, `docs/superpowers/design/ui-overhaul/phase2/manifest.json`.

## Owner decisions

- Layouts are reskin only (no geometry changes; the layout tests are untouched).
- Gothic titles (`title_gothic`) for the Grimoire and Blood Moon, plain titles (`title`) for Tensura.
- One icon per skill (`icons/{16,32}/<skill id>`).
- The HUD and boss bars follow the hub theme (phases 4 and 6; the kit and `ReduceMotion` they need are built here).
- Reduce motion lives in the hub Settings and covers UI animations only (not world animations such as boss shifts or particles).
- Contrast stays at least 4.5:1, focus rings stay, narration stays.

## Global Constraints

- **Pixel grid:** a sprite is drawn at its own size, doubled, or (a 16 px icon) halved; a nine-slice or tile scales only through its `.mcmeta`. No fractional scale of art.
- **Performance:** one texture blit per piece, no fill per pixel. Existing hand-drawn emblems (sigil, magic circle, moon) are not in the kit and stay as they are.
- **Missing sprite:** every `UiKit.draw` has a flat fallback in the theme's colours; the old `icons.png` atlas is the icon fallback. A pack that removes a sprite never crashes a screen.
- **Contrast:** every text colour is checked on the *picture* it is drawn on (the middle of the kit panel, inset and button faces), not only on the colour the theme names.
- **Reduce motion:** with it on, every tween is finished in its first frame, every pulse is held at its brightest band, and a state change still lands at once. The cooldown sweep is information and does not use it.
- **Plan 18 is built in parallel and adds hub actions:** the changes are kept in the drawing layer (`shared/hub/kit`, the three theme classes, `KitDraw`) so merges stay small.
- **No new mixins.** Gradle runs with `--no-daemon` and `--max-workers=2`, output to files, one at a time. Hidden shots only (`runSafeShots` under `scripts/dev/focus-watch.ps1`); never a visible client.

## Review Focus

1. **Missing texture / resource pack:** `UiKitFallbackTest` (presence asked once and forgotten on reload) and a hidden shot with the kit's sprites removed (flat colours, nothing crashes).
2. **Reduce motion on:** `ReduceMotionTest`; the Grimoire tab slide is the one live user of `UiMotion` in this phase and is checked in both states.
3. **Scale 4:** titles in the 16 px fonts that do not fit one line fall back to the normal font wrapped; nothing is cut off. Shots at scales 2 and 4.
4. **Tensura not installed with `hub_theme = TENSURA`:** the Grimoire shows with its kit (the theme-selection test of plan 16 is unchanged).

## File map

**Create (pure, `shared/hub/kit/`):** `Easing`, `Motion`, `AnimClock`, `ReduceMotion`, `KitSprites` (`Part`, `Flat`, `IconPlan`), `SpritePresence`, `KitMetrics`, `KitFaces`, `TitleFont`.
**Create (client, `shared/hub/kit/client/`):** `UiKit`, `UiMotion`, `KitWidgets`.
**Modify:** `KitDraw` (icons, skill icons, button, pips, focus ring, detail buttons, dialogs), `KitClient` (reload), `HubTheme` (`kit()`, `titleFont()`), `GrimoireTheme`, `BloodMoonTheme`, `TensuraHubTheme`, `PageKit.title`, `BookSkillsPage` (skill icon), `ElijahDealScreen` (themed dialog), `HubClientConfig` + `PlayerSettings` + `en_us.json` (`reduce_motion`).
**Tests:** `EasingTest`, `MotionStateTest`, `ReduceMotionTest`, `UiKitSlicesTest`, `UiKitFallbackTest`, `KitMetricsTest`, `KitFacesTest`, `TitleFontTest`; `ThemeContrast` (shared helper of the three theme tests) now also checks the kit palettes.

## Tasks

### Task 1: Motion rules (pure)

- [x] `Easing` (linear, out quad, out cubic, four steps), `Motion` (the durations of the style guide's event table, `progress`, whole-texel `texels`, the four glow bands), `AnimClock` (per-element state, a first sighting is at rest), `ReduceMotion` (the switch the config part installs).
- [x] `EasingTest`, `MotionStateTest`, `ReduceMotionTest`.

### Task 2: Kit names and fitting (pure)

- [x] `KitSprites.Part` lists every theme sprite the code draws, with its flat look and the transparent margin of its picture; `iconPlan` fits an icon to a box; `skillIcon` maps a skill id to its own icon or the "unknown" rune; `SpritePresence` remembers what exists until a reload.
- [x] `KitMetrics` (bar inset, fill, scroll thumb, rung and task parts), `KitFaces` (the face colour of every kit button and the label colour on it), `TitleFont` (the glyph list of both fonts).
- [x] `UiKitSlicesTest` (every part in the three kits, every slice valid for its picture, every icon and every skill at both sizes), `UiKitFallbackTest`, `KitMetricsTest`, `KitFacesTest` (the table equals the PNG), `TitleFontTest` (equals both font files).

### Task 3: Client layer

- [x] `UiKit` (draw a part with the flat fallback, the icon, the focus ring), `KitWidgets` (rule, bar, scroll bar, rung mark, task box, stamp), `UiMotion` (clock on the client tick, reads `ReduceMotion`).
- [x] `KitClient` forgets the presence cache on a resource reload.

### Task 4: `KitDraw` and `HubTheme`

- [x] `KitDraw.icon` draws the new sprites (the atlas is the fallback), `skillIcon`, the small icon `button`, `pips`, the focus ring (through the active theme's kit), the default `detail` buttons and the confirm dialog.
- [x] `HubTheme.kit()` and `titleFont()`; every theme sets `KitDraw.activeTheme` in `drawFrame`.

### Task 5: The three themes

- [x] Grimoire: cover, page panels, tabs (with the tab slide), wax seals, theme and close buttons, scroll bar, marks, rule, bar, focus.
- [x] Blood Moon: cover sky (the picture is 272 rows, the rest is its last band), cards, round rail buttons, detail bar and pills, marks, rule, bar, scroll bar, focus.
- [x] Tensura: cover and title bar, panels, list rows as tabs, buttons, pane buttons, task cards, marks, rule, bar, scroll bar, focus.
- [x] Page titles in the theme's bitmap font when every character is in it and it fits on one line (`PageKit.title`).
- [x] The skills page shows one picture per skill.

### Task 6: `reduce_motion`

- [x] Client config key `reduce_motion` (default off) in `HubClientConfig`, a Settings row through `PlayerSettings` and `en_us.json`; `AdminCoverageTest` and `HubLangCoverageTest` stay green.

### Task 7: Verify

- [x] `./gradlew.bat test` green (1914 tests, 0 failures; each new rule was reverted once and its test failed).
- [x] The 12 GameTest runs (all green: 1548, 1051, 29, 371, 1436, 1442, 1421, 1538, 1467, 1532, 1233, 270), one at a time with `--max-workers=2`: default, `-PnoWerewolves -PnoCurios`, `-PnoLinkedMods`, then `-PpartsOff=` with each of `species`, `artifacts`, `speciesTensura`, `supernatural`, `supernaturalTensura`, `hud`, `celestials`, `bosses`, and `species,artifacts`. Restore `run/config` after any killed run.
- [x] Hidden shots of every hub page in the three themes at GUI scales 2 and 4, plus the HUD; opened and checked for blur, misalignment and low contrast; fixes made.
- [x] Push, PR into `main`, green CI. Not merged.

## Left for phases 4 to 6 (listed, not built)

| Phase | What stays on the old drawing |
|---|---|
| 4 HUD | `SlotArt` slot frames, key badges, mastery bars, the blood ring, the cooldown pie (sprites `slot*`, `key_badge*`, `mastery_*`, `shared/*` exist), `HudChrome`, Alt panels; `UiMotion.glowPercent` for the ready glow and the press pop |
| 5 Hub screens | Every page's own drawing: source tags (`chip*`), the skill medallions and nodes (`node_*`), cards and rows still using `KitDraw.shape`, `emblem` and `cover_art`, corners and flourishes, page-turn and press motion, hover states of tabs (`tab_hover`), `button_pressed` and `tab_disabled`, the layout tests moving to the approved geometry, icon sizes of 16 where a page still asks for 12 |
| 6 Boss bars | `boss_*` sprites, `BossBarView`, the health tween (`Motion.BOSS_HEALTH`) |

## Shots

66 hidden shots (3 themes x 10 pages x scales 2 and 4, plus the HUD in 3 themes x 2 scales) are in un-safeshots/final-shots/ (git-ignored); a selection is in docs/superpowers/design/ui-overhaul/phase3/, and 
o-kit/ shows the flat fallback with the Grimoire sprites removed. The ability HUD is unchanged in this phase (its slot art is phase 4).

## Review round

Fixes after the PR panel: an icon name a resource location cannot hold (a skill of another mod) shows the unknown rune and never throws; a large nine-slice is drawn as its border plus one fill of its solid middle (`NineSlicePlan`, read once from the PNG), a few dozen draws and not thousands; the HUD halo honours `reduce_motion` (`Motion.pulse`); the "more below" marker has its own strip under the clipped content (`ScrollHint`); a skill the player has is not offered for purchase; the Blood Moon Guide lists end above the floating bar; the empty state drops its hexagon when the page is short (`EmptyFit`); the Blood Moon stage titles use the gothic font; the Tensura columns have a gap for the scroll bar; a cut skill name is read whole in a tooltip. The 12 GameTest runs of the first pass stayed green; this round changed only client drawing and pure helpers (CI runs them again).

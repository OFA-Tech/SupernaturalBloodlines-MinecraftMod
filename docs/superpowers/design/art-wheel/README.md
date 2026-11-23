# Skill wheel sprites (after the W3 mockups)

The new sprites the approved skill wheel mockups need. Hand-drawn pixel art, one texel per GUI pixel, in the kit's palettes
(`ui-overhaul/phase1/palettes.json`) and with the kit's canvas (`ui-overhaul/phase2/src/px.py`). Nothing is wired into code: W4 does that.

- Contact sheet: `contact-sheet.png` (per theme: the wheel of the mockup's first frame at 4x, 2x and 1x, then every new sprite alone on
  the band, the lit band and the inner ring).
- Generator: `py docs/superpowers/design/art-wheel/wheel.py` redraws every file below, adds them to `ui-overhaul/phase2/manifest.json`,
  runs the checks and redraws the contact sheet.

## Sprites

Paths are under `src/main/resources/assets/supernaturalbloodlines/textures/gui/sprites/`. `<theme>` is `grimoire`, `bloodmoon`, `tensura`.

| Sprite | Path | Size | Scaling | How to draw it |
|---|---|---|---|---|
| `wheel_cell_36` | `kit/<theme>/wheel_cell_36.png` | 36 x 36 | fixed | The frame of an outer ring power. The 32 px icon goes at +2, +2; its 28 px live area is the well. |
| `wheel_cell_36_active` | `kit/<theme>/wheel_cell_36_active.png` | 36 x 36 | fixed | The same frame for a power that is on. |
| `cell_glow_52` | `kit/<theme>/cell_glow_52.png` | 52 x 52 | fixed | The halo of a power that is on. Draw it first, at -8, -8 from the cell. Four flat alpha steps (60, 32, 16, 8 %), filled under the cell. |
| `wheel_edge` | `kit/grimoire/wheel_edge.png` | 3 x 3 | fixed | Brass rivet, centred on a spoke end. |
| `wheel_edge` | `kit/bloodmoon/wheel_edge.png` | 8 x 1 | tile (`.mcmeta`) | Rim hairline, laid along the rim outside the outer ring. |
| `wheel_edge` | `kit/tensura/wheel_edge.png` | 2 x 2 | fixed | Vertex tick, centred on an outer vertex. |
| `wheel_caret` | `kit/<theme>/wheel_caret.png` | 7 x 5 | fixed | The mark of the open path. It points up; turn it in code. |
| `cooldown_sweep_32` | `kit/shared/cooldown_sweep_32.png` | 32 x 1056 | frame strip | 33 frames of 32 x 32, frame 0 empty, frame 32 full, same layout as `cooldown_sweep`. Draw the frame at +2, +2 from the cell. |
| `lock_outline_42` | `kit/shared/lock_outline_42.png` | 42 x 42 | nine-slice, border 5, 5, 5, 5 (`.mcmeta`) | The dashed outline of a locked cell, at -3, -3 from the cell. |
| `infinity_stones` | `icons/16/infinity_stones.png` | 16 x 16 | fixed | Mod icon of the inner ring: six stones in a dark setting. |
| `great_sage` | `icons/16/great_sage.png` | 16 x 16 | fixed | Mod icon of the inner ring: an open book under a star. |

## Decisions

- **Cell shape.** The mockup stretched the 28 px HUD slot. The real frame keeps each theme's slot material but fits a 32 px icon:
  Grimoire is the chamfered page tile (gilded when on), Blood Moon a round-cornered stone, Tensura a hexagon with its side points cut flat.
  The frame is 4 texels thick, so the well is exactly the 28 x 28 live area of a 32 px icon.
- **Blood Moon outline.** The kit's `edge` colour is 1.8:1 on the dark band, so the idle cell is outlined in `moon5` (4.1:1) and the
  active cell in `hot`.
- **Blood Moon hairline.** The mockup drew it in `redDD` (1.3:1 on the void). The sprite is `red` (3.5:1); tint it down in code for the
  fainter look, a sprite cannot be tinted up.
- **Cooldown sweep.** A sprite, not code-drawn: a 28 px disc (the mockup's wedge over a 27 px disc), black at alpha 175 like the 16 px
  strip. The disc sits inside the well of all three themes.
- **Locked outline.** One shared sprite has to read on parchment and on the two dark themes, so the dashes alternate light and dark
  steel, four texels each. The corners are light. The 32 px middle tiles without a seam.
- **Halo.** Grimoire uses `ready` (the gold of the kit's active slot), not `glow`, which is lost on parchment.
- **Mod icons.** Neither is a drop, a disc, a cauldron or a slime, the shapes of the five existing `tag_*` icons, and neither reuses the mockup's
  stand-ins (`spark`, `eye`). They exist at 16 px only, as asked.

## Contrast (the 3:1 non-text rule)

The tone that carries each sprite against the grounds the wheel draws it on. `wheel.py` fails if any pair is under 3.
Grounds: Grimoire `paS` band, `paH` lit, `paD` track, `le` inner ring; Blood Moon `cardD`, `cardH`, `card`; Tensura `panel`, `panelH`,
`panelD`. The mod icons are checked against the well of the 28 px slot they sit in.

| Sprite | Grimoire | Blood Moon | Tensura |
|---|---|---|---|
| `wheel_cell_36` outline, band / lit | 3.6 / 5.3 | 4.1 / 3.5 | 7.9 / 6.1 |
| `wheel_cell_36_active` outline, band / lit | 8.1 / 12.0 | 7.8 / 6.6 | 11.6 / 9.0 |
| `wheel_caret`, lowest of four grounds | 6.0 | 11.9 | 9.0 |
| `wheel_caret` fill on its own outline | 8.1 | 14.3 | 14.7 |
| `wheel_edge`, lowest | 8.1 | 3.5 | 11.6 |
| `lock_outline_42`, lowest | 7.8 | 13.9 | 9.5 |
| `infinity_stones`, the darkest stone (reality) | 3.2 | 3.9 | 3.5 |
| `great_sage` pages | 15.5 | 18.5 | 16.6 |

Not in the table: `cell_glow_52` and `cooldown_sweep_32` are overlays with stepped alpha. Neither carries a state alone: "on" also has
the gilded frame and the ON pill, a cooldown also has its seconds.

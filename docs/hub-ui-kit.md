# Hub UI kit (plan 16a)

The shared pieces every hub theme is built from. Package `net.ofatech.supernaturalbloodlines.shared.hub.kit`; client only for the drawing classes, pure for the rest. The design is in `docs/superpowers/design/plan16/`; the plan is `docs/superpowers/plans/2026-11-16-ui-overhaul.md`.

## Pure (unit tested)

| Class | What it does |
|---|---|
| `TickRing` | The ticks of a progress ring, from the top, clockwise; `lit(count, fraction)` |
| `Radial` | Nodes round a centre (radial skill tree) and the arrow keys' move between them |
| `Shapes` | Pixel rows of the medallion shapes: `ROUND`, `HEX`, `SQUARE` |
| `PackRing` | The pack portrait ring: roles ALPHA, BETA, PACKMATE, BROTHER, SUBORDINATE (small, faint at 1/2) with their colours; plans 14 and 15 only add members |
| `SourceTag` | SB, VMP, WW, ENC, TEN from a mod or source id |
| `FocusOrder` | Zones NAV, STAGE, PANEL, ACTIONS from a target's id prefix (`tab:`, `seal:`, `theme`, `close` / default / `panel:` / `detail:`, `action:`); the number keys 1 to 8 |
| `ConfirmModel`, `ConfirmLayout` | The confirm dialog: Esc cancels, the safe button has the focus first, Enter alone never confirms |
| `ModelDrag` | Turning the emblem's model by dragging (vanilla's follows-mouse renderer, so the point it looks at moves) |
| `Detail` | The contract a page gives its theme for the selection: title, source, pips, text, fields, reward, actions (at most one primary) |
| `Icons` | The sprite atlas cells, in the order of `icons.txt` |
| `FocusRing` | The focus ring colour for a paper: gold on dark, deep red on light, 3:1 or more |

`Contrast.bestText(bg)` (white or black) is for labels on coloured chips.

## Drawing (client)

`KitDraw` draws them in a theme's colours: `icon`, `shape`, `medallion`, `tickRing`, `pips`, `sourceTag`, `focusRing`, `button`, `model` (vanilla `InventoryScreen.renderEntityInInventoryFollowsMouse`), `emblem` (ring round model), `detail` (the default look of a `Detail`) and `confirm`. A widget gets what it needs from a `UiContext` (the graphics, the font, the mouse, the focus, and `target(...)` to register something pressable); `HubScreen.Frame` implements it.

## The icon atlas

`src/main/resources/assets/supernaturalbloodlines/textures/gui/icons.png` is 16 x 4 cells of 16 px, white on transparent (a theme tints it); `icons.txt` names the cells in order. Both are written by `py tools/gen_icons.py` (standard library only). Adding an icon: draw it in the script, add its name to `NAMES` and to `Icons` in the same position, run the script; `IconsTest` fails when the two disagree or a named cell is empty.

## The theme API

`HubLayout` has a `Chrome` as well as the regions, tabs and seals: the theme switch button, a close button, the emblem rectangle, the detail rectangle and kind (`SIDE_REGION`, `PAGE`, `BOTTOM_BAR`, `INFO_PANE`) and the nav style (`INDEX_TABS`, `ICON_RAIL`, `LIST`). A rectangle that is null means the theme has none of it. The four-argument constructor stays, with an empty chrome.

`HubTheme` has default methods for the new chrome (`drawThemeButton`, `drawClose`, `drawDetail`, `focusColor`), so an old theme needs no change; a theme restyles them when it is rebuilt (16b to 16d).

`HubScreen`: draws and registers the chrome; the theme button cycles Grimoire, Blood Moon, Tensura (skipping a look that cannot be drawn) and saves it to `supernaturalbloodlines-hub-client.toml`, the same setting as the config; number keys 1 to 8 jump to a page; Tab follows the zone order; `Frame.detail(Detail)` hands a selection to the theme; `Frame.confirm(title, body, yes, action)` asks first (a dialog takes every key while it is open).

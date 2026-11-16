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

## API after the 16a review

- **Page first.** `HubPageView.keyPressed(key, modifiers)` and `mouseDragged(...)` (default: not used). `HubScreen` order: dialog, the hub key (always wins, even bound to a digit), the page, the number keys 1 to 8 (only with no Ctrl, Alt, Shift or Super), then the global arrows, Tab and Enter. A page that uses Left and Right (a radial tree, a ring) answers them and the hub no longer turns the page.
- **Emblem.** `HubTheme.drawEmblem(UiContext, Rect, EmblemData)`; `EmblemData` has the ladder progress (`HubModel.progress`), the rungs, the entity and the `ModelDrag` the screen owns. A page calls `Frame.emblem(Rect)` (or `Frame.emblem()` for `Chrome.emblem`); a drag that starts on it turns the model whichever theme drew it.
- **Counters.** `HubTheme.drawCounters(UiContext, Rect, Counters)` into `Chrome.counters` (SP always, EP with Tensura): a margin note, a top-right pair or a stats line, as the theme places it.
- **Detail.** `Detail` has a `kicker`, an `icon`, task `rows(label, SourceTag, done, total)`, and per action a `confirm(title, body)` and a `disabledReason`. `KitDraw.detailHeight(font, w, d)` measures the text; `DetailLayout.stack/textArea` place the actions and keep text out from under them; `KitDraw.detailActions(ctx, theme, rects, d)` registers the `detail:` targets, tooltips (with the reason), the confirm routing and the focus rings, so a theme draws its own buttons and calls it. The default `KitDraw.detail` no longer lets the text run under the actions or the title run into the tag.
- **`Frame.detail`** sets the current region and clip aside and puts them back, so a bar outside MAIN shows and takes clicks.
- **Confirm.** `confirm(title, body, safe, yes, action)`: the safe button (usually "Stay") is on the left and is focused first; the height follows the wrapped body up to 70% of the screen; only a left mouse click answers; nothing scrolls while it is open. An action with a `Detail.Confirm` goes through it by itself.
- `HubLayout.Chrome` is now (theme button, close, emblem, counters, detail); the unused nav style and detail kind were removed.
- `KitDraw` and `UiContext` are in `shared.hub.kit.client`.

## Dev screenshots (no clicking)

`-Dsupernaturalbloodlines.devshots=<theme>:<page|all>,...` (themes `grimoire`, `blood_moon`, `tensura`; pages by name) makes the client open the hub on each theme and page after the world loads, wait twelve ticks and grab the frame with vanilla's `Screenshot.grab` into `run/screenshots/devshots/<theme>-<page>-s<scale>.png`. Extra properties: `.scales=2,4` (GUI scales), `.size=1920x1080` (the window; 1080p at scale 4 is the 480 x 270 GUI the design is checked at), `.world=New World`, `.quit=true`. It sets the tutorial off. It is registered only when the property is set and lives in `DevShots` (client only); without the property nothing of it runs, and the server never loads it. Run it with `JAVA_TOOL_OPTIONS` and `gradlew runClient`.

## The Grimoire's book pages

`BookKit` (drop cap, leaders, wax seal, red link, footnote, small text, face) and `BookSkillsPage`, `BookQuestsPage`, `BookRitesPage`, `BookBloodlinePage`, `BookPackPage`, `BookGuidePage` are the Grimoire's own views; `GrimoireTheme.view(page)` returns them and the other two looks keep the shared pages. Path is `PathPage(true)`.

### Demo data for the dev shots

-Dsupernaturalbloodlines.devshots.demo=true makes DevShots set HubView.devOverride to DevDemo::apply: a 12 member pack, a family, two genes, rite items (and a glass bottle given to the dev player) and learned skills, so every populated state can be photographed. Client only, inert without the property, the server never sees it.


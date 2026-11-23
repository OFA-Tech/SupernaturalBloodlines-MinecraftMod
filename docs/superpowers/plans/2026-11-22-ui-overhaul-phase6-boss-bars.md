# UI overhaul phase 6: boss bars

> **For agentic workers:** execute task by task with TDD; every test must fail when its logic is reverted.

**Goal:** our nine bosses get their own bar, drawn from the phase 2 kit in the look of the hub's theme: a card with a portrait per form, a name plate, a health bar with ticks, the health in numbers and a form indicator that changes while a boss shifts. Other mods' bars and the Alpha Trial's own bar stay vanilla.

**Inputs:** roadmap phase 6, the spec (boss bars, motion, reduce motion), `design/ui-overhaul/phase1/boss-*.png` (the approved card: 250 x 46, plate 34 x 34 with a 24 x 24 portrait, name plate 200 x 13, bar 200 x 9 with ticks at 25, 50 and 75 %, chips 10 high, 6 px between bars), the kit (`boss_frame`, `boss_portrait_plate`, `boss_portrait_plate_shifting`, `boss_name_plate`, `boss_bar_track`, `boss_bar_fill`, `boss_bar_fill_shifting`, `boss_bar_tick`, `chip`, `chip_on`, `chip_shifting`) and the 15 portraits in `textures/gui/boss/`.

## What vanilla and NeoForge give us (checked in the 1.21.1 classes)

- A boss bar reaches the client as `ClientboundBossEventPacket`: a **random** UUID, the name, the progress (0 to 1), a colour, an overlay and three flags. It carries no boss id, no form and no health number. So the client cannot tell which boss a bar is, or its form, from the packet.
- `BossHealthOverlay.render` walks its `events` map (a `LinkedHashMap`, in the order the bars were added) from `y = 12`. For each bar it posts `CustomizeGuiOverlayEvent.BossEventProgress(x, y, increment)`; a **cancelled** event skips the vanilla drawing; the next bar starts at `y + getIncrement()` (`setIncrement` is public); and the loop **stops once `y >= guiHeight / 3`** (270 high at scale 4: after the second bar of 52 px). A cancelled event is not delivered to listeners that do not ask for cancelled events (the HUD's `BossBars` does not).
- `events` is package-private. One access transformer line (reads only, like the four already there) opens it; **no mixin**.

## Decisions

1. **The form travels in a payload, not in the name.** `BossBarInfoPayload(bar UUID, boss, look, flags, hp, max)` goes from `BossBars` (server) to each viewer: **before** the vanilla add packet (so the client never draws a vanilla bar first), when the form, the health or the fallen flag changes, and when a player comes into range. `BossesPart`'s registrar goes from `"1"` to `"2"` (optional, so an older client only loses the card) and the CHANGELOG says so.
2. **Our bars are drawn in one pass at the first of our events.** All of ours are cancelled; the first of them carries the whole stack's height as its increment and the others carry 0, so another mod's vanilla bar starts under our stack, and the loop's one-third rule can no longer drop our second and third bars (it would at GUI scale 4). The stack shows as many bars as fit in 60 % of the screen's height (three at scale 4); the rest are left to vanilla.
3. **The bar follows the hub theme** through `HudStyle.current()` and `HudKit` (shared, so the bosses part needs neither the hub nor the HUD). The look changes live.
4. **The health in numbers and the fill come from the payload**, not from vanilla's 100 ms lerp: one source for both. The fill eases over `Motion.BOSS_HEALTH` ticks.
5. **Forms:** Klaus, Deucalion and Peter: HUMAN, SHIFTING, BEAST. The Beast of Gevaudan: HOST, BEAST, SPIRIT (only BEAST has a bar today: the Host is calm and the Spirit is not a `BossNpc`). The other five bosses have no chips. A boss's form change calls the bar update at once (`BossNpc.barKey`), not at the next 10 tick pass.
6. **A fallen Original (desiccated)** keeps the vanilla colour's meaning: its fill is the gold one, the name says "(desiccated)".
7. **Reduce motion** (the setting from phase 3): no health easing (the fill is already there), no pulse on the shifting fill, no flash on the seam of the portrait, no fade between chips. The colours, the chip and the portrait still change, in one frame.

## Tasks

1. **Kit parts** (`KitSprites.Part`): `BOSS_FRAME`, `BOSS_NAME_PLATE`, `BOSS_BAR_TRACK` (margin 1), `BOSS_BAR_FILL`, `BOSS_BAR_FILL_SHIFTING` (tiles), `BOSS_BAR_TICK`, `BOSS_PORTRAIT_PLATE` and `_SHIFTING` (margin 4), `CHIP_SHIFTING`. `SCALED` holds the ones drawn at another size; flat stand-ins exist for each. `KitFaces` knows the face of the frame, the name plate and the three chips (held equal to the PNGs by `KitFacesTest`).
2. **Forms** (`core/boss/BossBarForms`): the look of a bar (which chips, which is lit) for a boss and a form; the lang key of each chip. A `BossNpc` says its look (`barLook`) and its key (`barKey`).
3. **Payload** (`BossBarInfoPayload`): a packed int for boss, set, active and flags; clamped on decode, so an unknown id is "no boss".
4. **Server** (`BossBars`): the info is sent before the add, on change (kept per bar, nothing is sent twice) and to a player who comes into range.
5. **Layout** (`BossBarLayout`, pure): the card's width for a GUI width, every element's rectangle inside the card, the chips right aligned with 2 px between them and clear of the health text, the fill width for a progress (a sliver for any health above zero), the tick positions, the stack (6 px gap), its height and bottom, the increments, and how many bars fit.
6. **Portraits** (`BossPortraits`, pure): the file of a boss in a form; Deucalion and Peter have no shifting portrait, so theirs is the human one and the wolf one split at a seam. All 15 files are reachable.
7. **Motion** (`BossBarMotion`, pure): health easing, the shifting fill's pulse, the seam's flash and the chip fade, each taking `reduce`.
8. **Client** (`BossBarClient`, `BossBarView`): the info per bar (bounded, pruned when the overlay no longer has the bar, cleared on logout and on a pack reload), the event handler, and the drawing: frame, portrait, plate, name, track, fill, ticks, health text and chips. Nothing is allocated or measured per frame: geometry, cut names, labels and widths are cached per (card width, language, text). Blend state is set on every blit. A portrait from a hi-res pack is read for its size once.
9. **HUD** (`parts/hud/client/BossBars`): reads cancelled events too, so the pill and the toast stay clear of our stack.
10. **Phase 5 leftovers.** (a) A label's colour on a surface comes from the pixel of the pack's own picture, not from a table of the shipped art (`UiKit` samples it once per part), so a recoloured pack still reads at 4.5:1; the table stays the fallback. (b) The Tensura Path pane lists every task of the next step: a row that does not fit whole is not drawn half, and the pane ends in "+N more".
11. **Dev shots:** `boss:<look>:<variant>` with `klaus_human|klaus_shifting|klaus_beast`, the same for `deucalion` and `peter`, `beast_host|beast_beast|beast_spirit`, `mikael`, `elijah`, `kol`, `rebekah`, `finn`, `stack3` (Klaus, Deucalion shifting, the Beast) and `stack3_other` (the same with another mod's vanilla bar), with the chat cleared first.

## Layout B: one focus card plus tokens (owner decision, replaces the vertical stack)

- **One row, one card tall, at the top centre, whatever the number of bosses.** The focus boss is the full card (portrait, name plate, health bar with ticks and number, form chips). Every other boss is a 38 px **token** in the same row, left and right of the card and kept balanced: the portrait plate, a 2 px **health ring** (clockwise from the top centre) and a **form mark** (the first letter of the lit chip, so the form is not told by colour alone). Too many for the width: the card narrows (down to 200) first, then the rest collapse into one "+N" token. Narration and the token tooltips list every boss.
- **Focus** (`BossFocus`, client side): the boss this player hit most recently (within 300 ticks), else the nearest. A switch that is not a hit waits 40 ticks (2 s) after the last one and needs the other boss to be about 20 % nearer. A hit switches at once. The facts come from two server messages: `boss_bar_info` now carries the boss's entity id (the client measures the distance), and the new `boss_hit` payload tells the player who damaged a boss (at most one per boss per 10 ticks, only to a player who watches that bar and has the channel). Both are inside the version "2" bump.
- **Switching focus:** the old card fades out and the new one in over 4 ticks (200 ms); with `reduce_motion` the card just changes.
- **Other mods' bars always start under our row.** The first of our bars tells vanilla it took 57 px (the card and the room for the next bar's name) and the others none, so vanilla's one-third rule (`VanillaBossLoopTest` simulates its loop) cannot drop another mod's bar at GUI scales 2, 3 or 4 with 1 to 9 bosses. (The old vertical stack was dropped at scales 3 and 4: two stacked cards already passed the third. That is why there is one row.) What is still vanilla's rule: bars of other mods that come **before** ours in the overlay push our row down, and five of them at scale 4 stop the loop before it reaches ours.
- **Status corner:** Tensura's and Vampirism's status ends 148 px from the left (measured at every GUI scale; `TrackerAnchor.CORNER_W`). With either mod loaded the row starts 4 px right of it where the screen is wide enough for a card beside it; the card narrows, never the other way.
- **Health ring colours** are the kit's own bar colours (held equal to the PNGs by `BossFacesTest`) and stand out from the ring's track at 3:1. A token has an opaque backing in the track colour so the ring stays readable on any sky.

**Note for Fable (art):** the token is built from existing kit pieces (the portrait plate, the chip, a plain frame for "+N") and a ring drawn with fills, so the plate (round) sits in a square ring. A square or hex token plate and a ring that follows the plate's shape would look better, and Peter's wolf portrait is very dark on the Blood Moon and Tensura plates.
## Not in this phase

- The transformation animations (phase 7). The shifting portrait here is a seam, not a morph.
- `AlphaTrialBoss`'s bar (the trial is not one of the nine bosses) and every other mod's bar.
- A bar for a boss that has none today (the Host, the Spirit).

## Verification

`./gradlew.bat test`, the 12 GameTest runs one at a time (`--max-workers=2`), and hidden shots of each boss and form alone, three bosses stacked, and three stacked with another mod's bar, in the three themes at scales 2, 3 and 4. Every scale 3 and 4 shot is opened in full and checked for overlaps, cut text, hidden content and contrast.

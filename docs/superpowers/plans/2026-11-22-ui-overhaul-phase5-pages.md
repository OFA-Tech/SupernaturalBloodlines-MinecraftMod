# UI overhaul phase 5: the hub pages on the kit

> **For agentic workers:** execute task by task with TDD; every rule gets a test that fails when the rule is reverted.

**Goal:** what the pages still draw themselves (buttons, text boxes, source tags, filter pills, cards, the skill nodes, the drop cap) is drawn from the phase 2 kit in the hub theme's look, and the page problems found in the phase 3 review are fixed. **Layouts are reskin only** (owner rule, memory 1210): no page moves a block, adds a row or changes what a page says. Where a kit sprite is bigger than the shape it replaces (the 34 px skill node), the page keeps today's smaller shape unless the room is there.

**Inputs:** roadmap phase 5, `docs/superpowers/design/ui-overhaul/phase1/`, the phase 2 kit (`button_*`, `button_primary_*`, `inset`, `plate`, `chip*`, `node_*`, `pip_*`, `cover`, `panel`), the phase 3 plan's "Left for phases 4 to 6" and "Found in the phase 3 review" tables.

## What stays as it is (already on the kit since phase 3)

The frame, tabs, seals, scroll bars, rules, bars, task boxes, rung marks, stamps, the focus ring, the detail pane and the dialogs.

## Tasks

1. **Buttons** (`PageWidgets.button`, `described`, `stepper`, `toggle`): drawn with `KitFaces.button(primary, enabled, hot, pressed)` and `UiKit.label`, so one place restyles every page button (Home, Profile, Pack, Admin, Settings, Guide links). A switch that is on is the primary part. A button that is off is never hot. The 4.5:1 label check of `KitFacesTest` covers them.
2. **Text box** (`PageWidgets.field`): the `inset` sprite; the warning outline of an invalid value and the accent outline while editing stay, as 1 px strips over it.
3. **Source tags** (`KitDraw.sourceTag`): the `chip` sprite with the label in the theme's ink, and the mod's colour as a 2 px line inside the chip's foot (so SB, VMP, WW, ENC and TEN are still told apart at a glance). The tag keeps its width.
4. **Pills and filters** (`BookKit.pillRow`, the Home identity chips): `chip` and `chip_on`.
5. **Cards** (`BookKit.outline`, used by the Blood Moon and Tensura Path nodes and Loadout cards): `plate` for a card, the accent as a 1 px line round the picked one. One helper, `BookKit.card`.
6. **Skill nodes** (`BookSkillsPage.node`): the 34 px `node_off`, `node_on` and `node_selected` sprites with the 32 px icon when the ring has room for them (`NodeSize`: the chord between two neighbours, less the gap, at least 34), else today's 18 px shapes with the 16 px icon. A pure rule, tested at the scales of the shots.
7. **Drop cap** (`BookKit.dropCap`): the Grimoire's chapter opens with a big capital that joins its word (`Vampire`, not `V ampire`): the first letter at twice the size in the accent, the rest of the word on the same baseline one pixel after it. Pure layout in `DropCap`.
8. **Quest progress pips** (`BookQuestsPage`): `pip_on` and `pip_off`.

## Page problems from the phase 3 review (fixed here)

| Page | Problem | Fix |
|---|---|---|
| Grimoire, Pack, s4 | `Dev · Alpha` runs into Ashen and Deaton; Lydia and Stiles run together | Names on the ring are placed by `RingLabels`: each label is clamped against the previous ones and the "you" line is dropped when it would touch a name (the ledger has it) |
| Tensura, Pack, s4 | The wheel's heads overlap | `PackRing.radiusFor(count, face)` keeps neighbouring heads at least 2 px apart (the ring grows, or the heads shrink to 10 px) |
| Grimoire, Guide | Every contents row reads `p. 14` | Each row shows its own chapter number (`ch. 3`); the spread's page number stays in the footer |
| Grimoire and Tensura, Skills, s4 | Ring labels spaced unevenly, cut (`Predat…`) with the name only in the tooltip; Silver Tongue and Venom Fang run together | `Radial.labelBox` gives each label the arc room between its neighbours and a bottom label goes under its node while the next one goes beside; a cut name is wrapped to two lines before it is cut |
| Grimoire, Quests | The task checkbox touches the first letter | 4 px gap (`PageKit.TASK_GAP`) |
| Blood Moon, Guide | The footer bar's hint is cut after two lines while there is free space above | The bar grows upward to the lines the hint needs (`FooterFit`), as far as the free space |
| Tensura, any two-column page, s4 | The scroll bar sits tight against the next column | 6 px gap between the bar and the column (`ColumnGap`) |

## As built

- **Cards** use the kit's inset (KitSprites.CARD): the plate is a one-line pill that bands when a card is tall. A card whose height is known only after its content (the Home request cards) is washed over the content at 15% (CARD_OVER_ALPHA, held to 4.5:1 by KitFacesTest).
- **Skill nodes**: NodeSize picks the kit's 34 px node where the ring has the room; at the shot sizes every ring was too small, so the 18 px shapes stay (reskin only). The 
ode_* sprites are wired and tested.
- **Skill ring names** that do not fit beside a node are cut with an ellipsis (the tooltip has the full name); one that fits nowhere is left to its tooltip (Silver Tongue at Blood Moon scale 3).
- **Pack ring** names are placed by RingLabels; the Dev · Alpha line is left out when a face stands in its way.
- **Pips** are the kit's pip_on/pip_off at 2x.

## Left for phase 6

Boss bars (`boss_*` sprites, `BossBarView`).

## Verification

`./gradlew.bat test`, the 12 GameTest runs one at a time (`--max-workers=2`), and hidden shots of the 10 hub pages in the three themes at scales 2, 3 and 4. Every scale 3 and 4 shot is opened in full and checked for overlaps, cut text, hidden content and contrast. The Grimoire kit is moved away once for a flat-fallback shot of Home and Skills.

# Boss art

Textures: `src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/`
Blockbench projects: `art/blockbench/bosses/<asset>.bbmodel` (Modded Entity format, texture embedded)
Previews (front | back | ¾, wolves also a head close-up): `art/previews/bosses/<asset>.png`
Generators (re-run to rebuild every texture): `art/bosses/tools/` (`paint_skins.py`, `paint_wolves.py`; Blockbench scripts `bb_*.js`).

All art is original. Palettes come from the characters' outfits, not from any actor.

## Player skins (64×64, classic arms unless noted, both layers used)

| File | Model | Palette | Notes |
|---|---|---|---|
| `mikael.png` | player | dark leather jerkin, grey fur mantle, linen, bronze buckle | Viking hunter, braided hair, full beard; white oak stake on the left hip (jacket + left pants layer) |
| `klaus.png` | player | olive henley, indigo jeans, gold/silver/cord necklaces | sandy tousled hair, stubble; three layered necklaces on the jacket layer; paint smudges on forearms and thigh |
| `elijah.png` | player | charcoal pinstripe suit, white shirt, wine tie | side-parted dark hair; lapels, pocket square and cuffs on the second layer |
| `kol.png` | player | cream shirt, burgundy waistcoat, tweed trousers, red tie | 1910s rogue: newsboy cap (hat layer), suspenders, rolled sleeves, belt knife |
| `rebekah.png` | **slim arms** | dusty blue gown, ivory lace sleeves, gold sash, wheat-gold hair | long hair on hat and jacket layers, full-length skirt on both pants layers |
| `finn.png` | player | undyed grey-brown wool, rope belt, brown leg wraps | 10th-century tunic, bone amulet, faint blue woad runes (forehead, forearm) |
| `deucalion_human.png` | player | near-black long coat, grey hair, silver buttons | dark round glasses wrapping the temples, goatee, coat tails on the pants layers. The cane is a prop in the `.bbmodel` only; its two small texture nets sit in unused skin space at (56,16) and (56,32) |
| `peter_hale.png` | player | charcoal henley, brown leather jacket, dark denim | short dark hair, stubble |
| `peter_hale_burned.png` | player | same, scorched | scar tissue on face and hands, one clouded eye, patchy hair, soot and holes in the second layer |
| `sebastien_valet.png` | player | burgundy justaucorps, gold braid, cream breeches, white stockings | 1760s French aristocrat: powdered wig with queue and ribbon, lace jabot and cuffs |
| `beast_host.png` | player | linen shirt, brown vest, grey trousers, worn red scarf | plain village man, the Beast's host |

## Werewolf-model textures (128×64)

All four use the Werewolves mod beast model (`WerewolfBeastModel`) UV layout exactly. Fur edges (neck, chest, jaw, sideburns) are cut out with transparent pixels like the original textures. Eyes are painted into the face; the mod's separate eye overlay layer, if used, stacks on top.

| File | Palette | Notes |
|---|---|---|
| `deucalion_demon_wolf.png` | soot black, ember-orange cracks, ash patches | burned look, red eyes |
| `peter_hale_wolf.png` | ashy charcoal-grey, pink burn scars, bone teeth | matted, feral, red eyes |
| `klaus_hybrid.png` | warm tan and brown, cream chest, black branching veins | yellow eyes |
| `beast_of_gevaudan.png` | black-brown fur, dull red veins on limbs, bone-white plates | brow band, skull cap, cheek plates, muzzle ridge, spine ridge, shoulder and forearm plates; white-blue eyes (per the Beast spec). Meant to render at about 2×, so details are 2–4 px blocks |

## Source of the werewolf UV layout

`de.teamlapen.werewolves:Werewolves:1.21-2.0.3.4` (Gradle cache). `WerewolfBeastModel.createBodyLayer()` was disassembled with `javap -c` and parsed by `tools/parse_model.py` into `tools/beast_model.json` (46 parts: pivots, rotations, boxes, texOffs, mirror). The same data builds the wolf `.bbmodel`s and is checked against the mod's own `beast_0.png`. Only layout numbers are used; none of the mod's textures are copied.

The `.bbmodel` wolves show the model's base pose with feet above the grid (the mod's renderer shifts it in game).

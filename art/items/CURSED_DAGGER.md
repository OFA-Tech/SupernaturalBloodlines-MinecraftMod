# Cursed White Oak Ash Dagger: placeholder art, for Fable

The item `cursed_white_oak_ash_dagger` (plan 18g, R205) has **placeholder art**: a purple tint of the plain dagger.

| File | What it is now |
|---|---|
| `src/main/resources/assets/supernaturalbloodlines/textures/item/cursed_white_oak_ash_dagger.png` | the plain `white_oak_ash_dagger.png`, recoloured (black-purple ramp by luminance) |
| `.../textures/item/3d/cursed_white_oak_ash_dagger.png` | the same recolour of the 3D texture |
| `.../models/item/cursed_white_oak_ash_dagger.json` and `..._3d.json` | copies of the plain dagger's models, pointing at the new textures |

To draw (Blockbench): black-purple ash and a faint violet glow, and **a different silhouette or glint, not just a different hue** (a tint alone is what the placeholder is; colour-blind players and a dark cave both lose it). Ideas: a notched or crooked blade, a split or barbed tip, a glinting edge on one side, a few violet motes. It must read as "cursed" **next to the plain one at 16x16** in the GUI, on the ground and in the hand. Keep the size and the hold of the plain dagger (the blades hit the same, and the same grip and length keep the first-person view alike); only the shape details and the glint may differ.

When it is replaced, nothing in the code changes: the file names above stay.

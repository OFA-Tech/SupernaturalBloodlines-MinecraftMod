# Cursed White Oak Ash Dagger: placeholder art, for Fable

The item `cursed_white_oak_ash_dagger` (plan 18g, R205) has **placeholder art**: a purple tint of the plain dagger.

| File | What it is now |
|---|---|
| `src/main/resources/assets/supernaturalbloodlines/textures/item/cursed_white_oak_ash_dagger.png` | the plain `white_oak_ash_dagger.png`, recoloured (black-purple ramp by luminance) |
| `.../textures/item/3d/cursed_white_oak_ash_dagger.png` | the same recolour of the 3D texture |
| `.../models/item/cursed_white_oak_ash_dagger.json` and `..._3d.json` | copies of the plain dagger's models, pointing at the new textures |

To draw (Blockbench): the plain dagger's silhouette, black-purple ash, a faint violet glow. It must read as "cursed" **next to the plain one at 16x16** in the GUI, on the ground and in the hand. Keep the same silhouette and the same 3D model proportions: the two blades hit the same, and nothing but the colour may tell them apart at a glance.

When it is replaced, nothing in the code changes: the file names above stay.

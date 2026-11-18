# Item art

Every item has two models: a 16x16 sprite for the GUI and a small 3D model for hands, the ground and item frames. `models/item/<id>.json` uses `neoforge:separate_transforms` (NeoForge 21.1): `base` is `<id>_3d`, `perspectives.gui` is an `item/generated` sprite of the existing `item/<id>` texture. Existing ids and texture paths are unchanged. Items are not registered in code. Sources of colours and materials: [REFERENCES.md](REFERENCES.md).

## Pipeline
1. `py art/items/gen_textures.py [--sheet out.png]`: sprites. Drawn in `icons.py` (uses `draw.py`) or as grids in `src/icons.txt`; 3D atlases of the stake and pike in `src/models.txt` (-> `textures/item/3d/`). Also recolours the 32 compass frames.
2. `py art/items/models3d.py`: 3D models as flat-colour box assemblies -> `spec3d/<id>.json` + a palette texture `textures/item/3d/<id>.png`.
3. `art/items/bb_helpers.js` (run inside Blockbench with `risky_eval`): builds one `item_<id>` Java Block project per spec, exports `<id>_3d.json` and `art/blockbench/items/item_<id>.bbmodel`, renders previews offscreen (`art/previews/items/<id>_gui|tp|ground.png`; `contact_sheet.png` from `sheet.py`).
   Props are recentred on (8,8,8) like a sprite so the vanilla `item/generated` transforms put them where a sprite sits; `fp_ref_gen` / `fp_ref_tool` previews (a vanilla sprite with generated / handheld transforms) are the first-person reference, and `first_person_sheet.png` compares every item with them (`sheet_fp.py`).
4. `py art/items/gen_models.py`: the `<id>.json` loader models; the compass keeps its `angle` overrides and each of its 32 frame models swaps the dial texture on the 3D case and the sprite.

## Items (45)
- **Blood and vials:** `original_blood`, `doppelganger_blood`, `hybrid_blood`, `sires_blood`, `klaus_blood` (red blood told apart by cork, band, flecks), `the_cure` (slim red vial, silver cap), `the_true_cure` (white-blue), `the_elixir` (gold flask).
- **Rings:** `sun_ring` (silver, lapis), `moon_ring` (silver, milky moonstone), `resurrection_ring` (silver, black onyx); the held 3D rings are small and oriented so the band wraps the end of the arm with the stone up (worn, not held like a tool). Worn-on-arm models for a Curios renderer are in `models/curio/` (12 files), see [RING_RENDER.md](RING_RENDER.md).
- **Poppets:** `sun_poppet`, `silver_poppet`, `spirit_poppet`: stitched cloth dolls.
- **Blades:** `silver_dagger`, `white_oak_ash_dagger`, `white_oak_stake`, `mikaels_stake`, `taser`, `valet_pike` (diagonal handheld transforms; the pike is held like a spear).
- **Gear and gems:** `hearing_device`, `vampire_compass` (brass case, animated dial on the 3D model too), `moonstone`, `memory_claws`, `mantle_of_the_beast`, `kol_grimoire`, `esther_grimoire`.
- **Plants and powders:** `vervain`, `vervain_seeds`, `vervain_tea`, `yellow_wolfsbane` (+`_seeds`, `_ash`), `northern_blue_wolfsbane` (+`_seeds`, `_ash`), `wolf_lichen`, `wolf_lichen_ash`, `white_oak_ash`, `mountain_ash_dust`.
- **Sparks:** `alpha_spark` (red eye), `true_alpha_spark` (red, gold flare), `npc_alpha_spark_0/1/2` (gold to orange-red as it grows; per-stage models, nothing selects between them yet).

Not covered: block items (logs, planks, leaves, saplings, `blood_stone`, `condensed_magicule`) already render from block models or plant sprites.

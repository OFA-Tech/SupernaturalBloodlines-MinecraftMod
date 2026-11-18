# Boss reward and spark item art

Textures are 16x16, built from the pixel grids in `src/` by `py art/items/gen_textures.py` (writes `assets/supernaturalbloodlines/textures/item/`). Models are in `assets/supernaturalbloodlines/models/item/`, Blockbench projects in `art/blockbench/items/` (`item_<name>.bbmodel`), previews in `art/previews/items/`. Items are not registered in code.

| Asset | Kind | What it is |
|---|---|---|
| `mikaels_stake` | 3D hand-held | White oak stake, gold collar, leather grip, red gem in the pommel; handheld display transforms (point forward). |
| `klaus_blood` | icon | Cork-stoppered vial of dark Original blood with gold hybrid flecks and a gold neck band. |
| `the_elixir` | icon | Round flask of glowing gold liquid under a red wax seal. |
| `kol_grimoire` | icon | Violet witch grimoire with silver corners and a teal closed-eye rune (cloaking). |
| `the_true_cure` | icon | Slim silver-capped vial of pale white-blue liquid with sparkles; distinct from `the_cure`. |
| `esther_grimoire` | icon | Brown leather grimoire with two linked gold rings (Linking Spell), page block and red ribbon. |
| `memory_claws` | icon | Three ivory claws rising from a cyan memory orb in a dark red cuff. |
| `valet_pike` | 3D hand-held | Long dark ash pike, leather grip, steel leaf blade etched with violet wolfsbane, gold fleur-de-lis guard; held diagonally like a spear. |
| `mantle_of_the_beast` | icon | Hooded shadow cloak with red eyes in the hood, gold clasp and smoky hem. |
| `alpha_spark` | icon | Glowing red wolf eye with vertical rays. |
| `true_alpha_spark` | icon | Same eye in ice blue with four diagonal sparks (silhouette differs from the red one). |
| `npc_alpha_spark_0` / `_1` / `_2` | icon, 3 stages | Weak orange-brown ember, half-open eye, nearly full eye without rays; the growth stages of the NPC spark. |

Notes for the code side: the three `npc_alpha_spark_N` models are plain per-stage models, and nothing selects between them yet (no predicate or item model definition is added). The two 3D models have no `parent`; they carry all their display slots and `gui_light: front`.

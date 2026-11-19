# Wolves: Deucalion's Demon Wolf and Peter Hale's Alpha

GeckoLib 4 assets for two Teen Wolf forms. Assets only: no Java, no dependency changes. Sources in `REFERENCES.md`.

| | Deucalion's Demon Wolf (S3) | Peter Hale's Alpha (S1) |
|---|---|---|
| Geo | `geo/entity/deucalion_demon_wolf.geo.json` | `geo/entity/peter_hale_alpha.geo.json` |
| Animations | `animations/entity/deucalion_demon_wolf.animation.json` | `animations/entity/peter_hale_alpha.animation.json` |
| Texture | `textures/entity/boss/deucalion_demon_wolf_geo.png` | `textures/entity/boss/peter_hale_alpha_geo.png` |
| Glow mask | `..._geo_glowmask.png` (eyes only, for `AutoGlowingGeoLayer`) | same |
| Blockbench | `art/blockbench/creatures/deucalion_demon_wolf.bbmodel` | `art/blockbench/creatures/peter_hale_alpha.bbmodel` |
| Size | 98 cubes, 25 bones, 256x256, 3 blocks tall (hunched), 1.9 wide | 198 cubes, 27 bones, 256x256, 2.5 blocks tall, 4.6 long |

All paths under `src/main/resources/assets/supernaturalbloodlines/`. Identifiers: `geometry.<name>`; animations are `animation.<name>.<anim>`.
Facing is -Z, left is +X, paws on y=0 (checked with a bounds pass in every animation, see below).

## Bones
**Deucalion** (upright, hunched): `root > hips > spine > (neck > head > (jaw, ear_l/r, hair), pec_l/r, arm_l/r > forearm > hand > fingers)`, and `hips > leg_l/r > shin > foot`.
Claws are 4 finger cubes plus a thumb per hand, toe claws on the feet. Rest pose: spine pitched 15, neck 10, head -28 so the face stays level.

**Peter** (quadruped): `root` (pivot at the hind paws, so `rise_up` pivots there) `> body > (neck > head > (jaw, ear_l/r), foreleg > foreleg2 > paw,
hindleg > shin > hock > foot, tail1 > tail2 > tail3, mane_a, mane_b, mane_chest)`.
The mane is about 100 loose tufts in three bones, swept back, so it sways independently of the body.

## Animations (every one has easing and the loops close exactly)
| name | length | loop | notes |
|---|---|---|---|
| `idle` | 3.6-4.0 s | yes | breathing, head sweep, snarl, tail/mane/ear drift |
| `walk` | 1.0 / 1.2 s | yes | Deucalion: lumbering contralateral stride with torso twist. Peter: diagonal prowl, head low |
| `run` | 0.6 s | yes | Deucalion: forward-thrown lunge. Peter: bounding gallop (fore pair, then hind pair) |
| `attack_swipe` | 1.0 s | no | wind-up (easeOutCubic), strike (easeInQuad), follow-through |
| `attack_bite` | 1.2-1.3 s | no | coil, lunge, jaw snap (easeInQuart), head shake |
| `roar` | 2.6 s | no | rear back, head up, jaws wide, arms or mane flared, then a ~1 s shudder |
| `hurt` | 0.6 s | no | recoil and snap-back |
| `death` | 2.4 s | hold | Deucalion: flinch, knees buckle, topples face down. Peter: forelegs buckle, rolls onto its side |
| `drop_to_fours` | 1.5 s | hold | Deucalion only: sink, throw the weight forward, hands slap down, quadruped crouch |
| `rise_up` | 1.6 s | hold | Peter only: rears onto the hind legs and holds |
| `pounce` | 1.2 s | no | Peter only: coil, leap (root moves 30 px forward), land on the forepaws. Needs the entity to ignore the root offset or move with it |

Animation values are offsets from the rest pose. Cycles use 12 samples per loop; one-shots use the GeckoLib easings
(`easeOutCubic`, `easeInQuad`, `easeInQuart`, `easeOutBack`, `easeInOutSine`, `easeOutBounce`).
The root position keys in `death`, `drop_to_fours`, `rise_up` and `pounce` keep the model on the ground, so
**the entity must not add its own vertical offset in those states**.

## Decisions
- **Deucalion has no fur and no wolf muzzle.** The wiki text and the owner's note both say "more demonic than wolf-like", and fans describe a prosthetic mask
  with red eyes. So: a human-sized skull with a heavy brow, short wrinkled snout, flared nostrils, tall pointed ears, near-black swept hair, and a body of
  blue-grey skin with painted sternum, pec and ab lines. He wears ragged charcoal trousers, because the show's shifted werewolves keep their trousers
  and because it hides the least-referenced anatomy. The eyes are a single red slab with one hot pixel, angled down toward the nose (the "angry" slant).
- **Peter is a real quadruped** (Heart Monitor script: "loping on all fours"; Allison's quadruped Gévaudan book). He stays plainly
  wolf-shaped, with a permanent snarl (jaw rests 14 degrees open, brow cubes angled down), and rises on the hind legs for the upright shots the show also used.
- **Colour.** Both keep the hue of the previous flat textures: Deucalion navy blue-grey, Peter black with a blue sheen. Peter's mane is the darkest
  value with blue-grey strand tips, so it reads against the body in a single glance. Claws are white and fade darker at the root.
- **Textures are painted per texel from the 3D position on each cube** (`tools/wolflib.py`), so strands run along the limb, veins and creases continue
  over cube edges, and the left and right sides match. Per-face UVs (not box UV) let the head, snout and chest use 2x to 3x texel density.
  Shading is baked lightly (top lighter, underside darker) because GeckoLib adds its own light.
- **Glow mask.** The eyes are flagged while painting and written to `*_geo_glowmask.png`; hook it up with `AutoGlowingGeoLayer`.

## Rebuilding
`python art/creatures/wolves/tools/deucalion.py <repo>` and `peter.py <repo>` write geo, texture, glow mask and animations (Python 3 + Pillow).
In Blockbench (MCP `risky_eval`): create a `geckolib_model` project, run `tools/bb_load.js`, then `tools/bb_shot.js` (offscreen renders) and
`tools/bb_bounds.js` (ground check). `tools/montage.py` makes contact sheets.

## Known limits
- Down faces use the box-UV orientation guess; undersides (soles, jaw underside) are low detail.
- Fingers do not articulate individually; they curl as one bone per hand.
- Peter's `rise_up` is a rear-up with the hind legs digitigrade; a fully plantigrade biped stance would need a second leg pose.
- Not yet run in the game; previews are Blockbench renders. Check the `death` and `pounce` root offsets in-game.

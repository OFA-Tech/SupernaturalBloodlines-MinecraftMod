# Klaus Mikaelson's hybrid wolf form

GeckoLib 4 assets for Klaus's wolf form, built the same way as the wolves in `art/creatures/wolves/` (same generator library, same
loader and preview scripts). Assets only: no Java, no dependency changes. Sources in `REFERENCES.md`.

| | |
|---|---|
| Geo | `geo/entity/klaus_hybrid.geo.json` (`geometry.klaus_hybrid`) |
| Animations | `animations/entity/klaus_hybrid.animation.json` (`animation.klaus_hybrid.<name>`) |
| Texture | `textures/entity/boss/klaus_hybrid_geo.png` |
| Glow mask | `textures/entity/boss/klaus_hybrid_geo_glowmask.png` (the golden irises only, for `AutoGlowingGeoLayer`) |
| Blockbench | `art/blockbench/creatures/klaus_hybrid.bbmodel` |
| Previews | `art/previews/creatures/klaus_hybrid_*.png` |
| Size | 114 cubes, 27 bones, 256x128; 2.1 blocks to the ear tips, 1.5 at the shoulder, 0.9 wide, 4 long with the tail |

All paths under `src/main/resources/assets/supernaturalbloodlines/`. Facing is -Z, left is +X, paws on y=0.
`geckolib_validate_model` (with animations): valid, 0 errors. Its only warning is a missing mod ID, which the exported files do not need.

## Bones
`root` (pivot at the hind paws) `> body > (neck > (head > (jaw, ear_l/r), ruff), foreleg > foreleg2 > paw, hindleg > shin > hock > foot,
tail1 > tail2 > tail3, chest_ruff, hackles)`.
The leg bones have no rest rotation: each segment is a cube swung about its joint, so the paws are flat on the ground by construction.
`hackles` and `ruff` bristle by **scale** from their roots.

## Animations
| name | length | loop | notes |
|---|---|---|---|
| `idle` | 4.0 s | yes | slow look around, ears turning, tail drifting, hackles breathing, a slow pant |
| `walk` | 1.0 s | yes | diagonal prowl, 9 px stride, head a little low |
| `run` | 0.5 s | yes | low gallop: fore pair lands, hind pair drives, a short flight after each; ears pinned, tail streaming |
| `attack_bite` | 1.2 s | no | crouch back with jaws wide, lunge (`easeInQuad`), snap shut (`easeInQuart`), shake |
| `howl` | 3.2 s | no | dip, sit back, muzzle to the sky, a held note with a tremble, hackles up |
| `death` | 2.4 s | hold | flinch, legs give way, rolls onto its left flank and goes still |

Values are offsets from the rest pose; loops close exactly (asserted in the generator).
Legs are solved with two-bone IK from paw positions, so standing paws stay on the ground and do not skate: Blockbench bounds
(`wolves/tools/bb_bounds.js`) read 0 to -0.2 px through `walk` and `run` and at every keyed pose of the others (-0.6 px for a moment
between two keys of `attack_bite`). `walk` covers 16 px per cycle and `run` 50 px per cycle; match the
entity's speed to that. `death` moves `root`, so the entity must not add its own offset or death rotation there.

## Decisions
- **A real wolf, not a monster.** The wiki describes wolf forms as natural wolves, only larger and more feral. Long legs, deep chest,
  tucked waist, thick ruff, bushy low tail; the hybrid shows in the face.
- **Hybrid face.** Golden iris on a black sclera, slanted angry; two upper fangs a side (the long vampire fang, a shorter wolf fang
  behind it); a soft dark tear-line under each eye where the veins would be.
- **Coat.** Ash-grey with a brown tint, grizzled, from the owner's show still ("pretty much like his hair colour, but brownish"): grey-cream face,
  cheeks, neck ruff, chest and belly; a darker saddle, muzzle top, ear tips and tail tip. How each colour was derived is in
  `REFERENCES.md`. The flat texture `klaus_hybrid.png` is left untouched.
- **Fur, not spikes.** The hackles and nape are overlapping slabs of long fur lying back along the spine; the neck ruff is two
  overlapping locks a side. They swell when the wolf bristles (bite, howl, the flinch of death) and lie low otherwise.

## Rebuilding
`python art/creatures/klaus/tools/klaus.py <repo>` writes geo, texture, glow mask and animations (Python 3 + Pillow, uses
`../wolves/tools/wolflib.py`). In Blockbench: make a `geckolib_model` project named `klaus_hybrid`, then run the wolves'
`bb_load.js`, `bb_bounds.js` and `bb_shot.js` and the bosses' `bb_save.js` on it.

## Known limits
- Not run in the game yet; previews are Blockbench renders. No Java entity, renderer or glow layer is wired up.
- No `hurt` animation (the wolves have one); not asked for here.
- One trunk bone: the spine does not flex in the gallop. The hock is not animated on its own.
- The `.bbmodel` stores the animation file's absolute path of the worktree it was saved from, like the wolves' files.

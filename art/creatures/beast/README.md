# La Bête du Gévaudan: GeckoLib model

Assets only, no Java. This is the second take on the Beast, which the owner picked over the first; it now sits under the first version's asset names, so nothing that refers to them needs renaming. The first version is in git history (last at commit `63b4613`).

| File | Path |
|---|---|
| Geo (`geometry.beast_of_gevaudan`, 25 bones, 71 cubes) | `src/main/resources/assets/supernaturalbloodlines/geo/entity/beast_of_gevaudan.geo.json` |
| Animations (10, GeckoLib format with per-keyframe `easing`) | `src/main/resources/assets/supernaturalbloodlines/animations/entity/beast_of_gevaudan.animation.json` |
| Texture (256x128) | `src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/beast_of_gevaudan_geo.png` |
| Blockbench source ("GeckoLib Animated Model", texture and animations embedded) | `art/blockbench/creatures/beast_of_gevaudan.bbmodel` |
| Previews (front, three-quarter, side, roar, rear_up) | `art/previews/creatures/beast_*.png` |
| References and palette | `REFERENCES.md` |

`textures/entity/boss/beast_of_gevaudan.png` (no `_geo`) is a different asset: the skin for the Werewolves-mod model. It is not part of this model and was not touched.

## Design

- **Built at final size**: 1 px is 1/16 block, so it needs no renderer scale. The hump is 47 px (2.9 blocks) high, the body 56 px long, the shoulders 36 px wide.
- **Shape**: a knuckle-walker. Huge shoulders and a hump above a low, broad, flat head; long thick arms set wide; a back that slopes down to heavy digitigrade hind legs.
- **Face**: slanted white-blue eyes under a heavy angled brow, a flat nose, and a lipless grin of needle fangs. The fangs are zero-thickness planes with transparent gaps (upper row on the head, lower row on the jaw), so the render type must be a cutout one. GeckoLib's default `entityCutoutNoCull` works.
- **Paler underside**: the chest, the lower flanks and the belly use the chest fur colours from the stills, a dark grey-blue, so the trunk reads from the side.
- **Shadow body**: ragged fur planes on the hump, shoulders and elbows, and a short smoke wisp instead of a tail. Each hump plane and the wisp has its own bone so the fur can bristle and, in `collapse`, shrink away.
- **No bone rest rotations**: every tilt is a cube rotation, so a bone's X, Y and Z are always the world's and animations stay easy to reason about.

## Bones

`root > body > waist > chest > neck > head > jaw`, with `ear_left/right` on the head; `mane_front`, `mane_crest`, `mane_rear` on the chest; `arm > forearm > hand` (left and right) on the chest; `leg > shin > foot` (left and right) and `wisp_tail` on the body. `body` moves everything; `waist` bends the spine and is what lifts the front in `roar` and `rear_up`.

## Animations

All names are `animation.beast_of_gevaudan.<name>`, the same ten as the first version.

| Name | Length | Loop | What happens |
|---|---|---|---|
| `idle` | 4.0 s | loop | Slow breath, the head watching left and right, a jaw twitch, ear flicks, bristling fur. |
| `walk` | 1.2 s | loop | Knuckle-walk in diagonal pairs, shoulders and hips counter-turning. |
| `run` | 0.7 s | loop | Bounding charge: both arms land, the spine curls, both legs drive. |
| `attack_bite` | 1.0 s | once | Coils back with the jaw wide, lunges (contact at 0.42 s), snaps shut, shakes its head. |
| `attack_swipe` | 1.1 s | once | Left arm rears up and rakes across (contact at 0.5 s). |
| `roar` | 2.6 s | once | Gathers low for 0.45 s, rears up on the hind legs with the arms flung wide and the head thrown back, bellows from 0.85 to 1.95 s with the head shaking, and lands at 2.3 s. |
| `rear_up` | 3.2 s | once | Crouches, stands on the hind legs with the arms overhead and the head down at its prey (1.1 to 2.3 s), slams down at 2.7 s. |
| `hurt` | 0.6 s | once | Recoil to its right with a snarl. |
| `death` | 3.0 s | hold | A last roar, the arms give way, it rolls onto its right side. |
| `collapse` | 3.4 s | hold | Phase 3: shudders, sinks onto its belly, and the fur planes and wisp shrink away as the shadow leaves. |

## Rebuilding

Everything is generated; the scripts in `tools/` are the source.

1. `node tools/model.js spec.json` writes the bones, cubes and UV packing.
2. In Blockbench, `bb_build.js` rebuilds the project from `spec.json` and writes `faces.json` (the UV rectangle of every cube face).
3. `python tools/paint.py faces.json <texture.png>` paints the texture; `bb_tex.js` loads it.
4. `node tools/anims.js spec.json <animation.json>` writes the animations; `bb_anim.js` loads them.
5. `bb_shot.js` renders previews through an offscreen view; `bb_save.js` writes the `.bbmodel`.

The `bb_*.js` files are run through the Blockbench MCP `risky_eval` tool as `(0,eval)(source)({fs, ...})`.

## Checks

- `geckolib_validate_model` (animations included): valid, 0 errors, 0 warnings.
- The geo is the GeckoLib plugin's own export (`geckolib_export_model`).
- The animation file is written by `anims.js`, not by the plugin: in this Blockbench session the plugin's export keeps every keyframe value (1038 of 1038 matched) but drops all 750 easings.
- Nothing sits below the ground in the rest pose (lowest point y = 0).
- Not checked: nothing here has been loaded in the game, and the animations were judged from still poses.

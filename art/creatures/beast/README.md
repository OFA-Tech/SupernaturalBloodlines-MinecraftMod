# La Bête du Gévaudan: GeckoLib model

Assets only, no Java and no GeckoLib dependency. Wire-up later: geo model `supernaturalbloodlines:geo/entity/beast_of_gevaudan.geo.json`, animations `.../animations/entity/beast_of_gevaudan.animation.json`, texture `.../textures/entity/boss/beast_of_gevaudan_geo.png`.

| What | Where |
|---|---|
| Geo (GeckoLib / Bedrock `geometry.beast_of_gevaudan`, 34 bones, 98 cubes) | `src/main/resources/assets/supernaturalbloodlines/geo/entity/beast_of_gevaudan.geo.json` |
| Animations (10, GeckoLib format v2 with `easing`/`easingArgs`) | `src/main/resources/assets/supernaturalbloodlines/animations/entity/beast_of_gevaudan.animation.json` |
| Texture (256x256, hand-painted) | `src/main/resources/assets/supernaturalbloodlines/textures/entity/boss/beast_of_gevaudan_geo.png` |
| Blockbench source ("GeckoLib Animated Model", texture embedded) | `art/blockbench/creatures/beast_of_gevaudan.bbmodel` |
| Previews | `art/previews/creatures/` (also copied to `%TEMP%\opencode\creature-art\beast\`) |
| Research | `art/creatures/beast/REFERENCES.md` |
| Generators | `art/creatures/beast/tools/` |

The existing `textures/entity/boss/beast_of_gevaudan.png` (the Werewolves-mod beast layout) is untouched. The GeckoLib texture has the `_geo` suffix so both can live side by side.

## Size and stance

Feet at y=0, front is -z, a unit is 1/16 block. The shoulder hump reaches about 3.3 blocks (fur tufts a little more), the head hangs at about 2.3 to 2.9, and it is about 4.8 blocks long from nose to rump plus the tail, roughly **2x** the Werewolves-mod model as the spec asks. It is a quadruped by default (long arms, digitigrade hind legs) and stands on its hind legs for `rear_up`. A hitbox of about 2.5 wide x 3.3 tall suits the all-fours form.

## Bones (parent > child)

```
root
 body                      hips, rump, tail and hind legs hang from it
  spike_back_1..3          dorsal fur tufts
  tail_1 > tail_2 > tail_3 (+ tuft)
  leg_l_upper > leg_l_lower > foot_l > paw_l (+ 3 claws)     leg_r_*  mirrors it
  chest                    barrel chest, shoulder hump, silver keel and belly fringe
   spike_shoulder_1..3     shoulder fur tufts
   neck
    mane_1..3              neck fur tufts
    head                   cranium, brow, muzzle, nose, cheeks, 10 upper teeth, 2 fangs
     ear_l, ear_r          two cubes each, pointed
     jaw                   jaw, chin fur, 10 lower teeth, 2 fangs, tongue
   arm_l_upper > arm_l_lower > hand_l (+ 4 claws)             arm_r_* mirrors it
```

- `root` rotates/moves the whole creature, `body` is the hips, `chest` the shoulder girdle. Rearing is done by pitching `body` and `chest` and counter-rotating the legs.
- `head` is the look-at bone for a GeckoLib `GeoEntityRenderer` (pitch/yaw on it).
- `l`/`r` follow the file convention (`leg_l_*` has x>0 in the geo json, as in the vanilla Bedrock humanoid). Blockbench shows the geo mirrored on x, so "left" is the entity's own left when it faces the viewer.
- Rest rotations are baked into the geo: hind legs, tail, neck, head and tufts are pre-bent. Animation keyframes are added on top of them.

## Animations

All names are `animation.beast_of_gevaudan.<name>`. Easing is per keyframe (the segment arriving at that key): `easeInOutSine` for cycles and recoveries, `easeOutQuad`/`easeOutBack` for wind-ups, `easeInQuad`/`easeInExpo`/`easeInCubic` for strikes, `easeOutBounce` for the body hitting the ground.

| Animation | Length | Loop | Content |
|---|---|---|---|
| `idle` | 4 s | loop | breathing (chest scale), slow head sway, jaw hanging a little open, ear flicks, tail and tuft sway |
| `walk` | 1.2 s | loop | diagonal-pair walk, body bob and sway, lifted hind shins, tail lag |
| `run` | 0.7 s | loop | bounding gallop: forelegs together, hind legs together, body pitch, tail streams back, jaw open |
| `attack_bite` | 1.0 s | once | crouch and rear the head (jaw 52 deg), lunge forward with a snap shut, recover |
| `attack_swipe` | 1.1 s | once | left arm winds back and out, chest twists, swings across (claws), recovers |
| `roar` | 2.6 s | once | crouch, then rise: chest swells, head up, jaw wide, head and jaw shake, tufts bristle, ears pinned back |
| `rear_up` | 3.2 s | once | crouch, rise onto the hind legs with arms lifted and spread, hold, then lower again |
| `hurt` | 0.6 s | once | recoil, head thrown back, snarl, quick shake |
| `death` | 3.0 s | hold last frame | rears, front legs buckle, drops and rolls onto its side, limbs go limp |
| `collapse` | 3.4 s | hold last frame | the 25% health collapse: stagger, forelegs fold, head sags, three slow laboured breaths |

`death` rolls the root, so the corpse is shifted about 1 block to the +x side. Rotate or offset the renderer if you need it centred.

## Design decisions and where they come from

(Numbers in brackets are rows of `REFERENCES.md`.)

- **No bone, skull or skeleton.** The brief suggested a partly skeletal Beast. The show stills and wiki show a fur-covered, smoky black beast and no bone armour [1, 2, 3], and the spec says there is no mask [10]. Bone-white is used only for teeth and claws.
- **Hulking, hunched, head low between a huge shoulder hump, long thick arms, small head** [2, 4]. The chest and hump are the biggest masses, forearms and hands are oversized, the neck is pitched down 19 degrees and the head a further 9 so it hangs below the hump.
- **Feline face, wide lipless mouth of many uneven teeth** [1, 3, 5]. A broad flat cranium, a short deep muzzle, the muzzle bottom edge painted as a dark lip-less seam, 10 uneven upper and lower teeth plus 4 fangs, pointed ears set high.
- **Eyes**: narrow, slanted toward the nose, glowing white-blue with no iris [1, 3, 11, 12]. Painted bright pale blue core on a darker blue socket.
- **Palette**: near-black blue-tinted fur, silver-grey on chest, belly and throat, dull red veins [1, 2]. Veins are kept thin and dim so they read as veins and not as lava. Fur is painted as vertical strands with clumped highlights; tufts have pale frayed tips so the silhouette looks matted and smoky.
- **Mane tufts instead of "bone spikes"**: ragged tufts along the neck, shoulders and spine echo the show's ridged matted fur and the historical beast's dark dorsal stripe [1, 9]. They bristle in `roar` and `rear_up`.
- **Short heavy tail** with a tuft, carried low [7, 9].
- **Digitigrade hind legs, crouch-and-spring movement** [4, 9]. The hind leg has thigh, shin, hock and paw so it can fold deeply for `attack_bite`, `collapse` and `death` and straighten for `rear_up`.
- **Rearing** like the fresco [6]; roar and collapse follow the spec's phases [10].
- **Gameplay hooks left to code, not baked into the art**: the black smoke aura is a particle job (the spec's "smoke body"), and the glowing eyes want an emissive layer. Eye pixels sit on the front face of the `cranium` cube, so a glow-mask texture is a small extra file when the renderer is written.

## Texture notes

- 256x256, box UV, about 1 px per model unit (vanilla density). Left and right limb bones share texels through mirrored UVs, so the left/right halves are identical on purpose.
- Painted by `tools/paint.js` from per-cube material tags in `tools/spec.js`: `fur`, `tuft`, `belly`, `skinfur`, `skin`, `bone`, `mouth`, `ear`. The painting is deterministic, so re-running rebuilds the same PNG.

## Rebuilding

The Blockbench scripts run inside Blockbench through `risky_eval` (Blockbench MCP). Order: `build.js` (outliner and UVs from `spec.js`), `paint.js` (texture), `anims.js` (animations), `shot.js` (offscreen renders), `export.js` (geo, animation and `.bbmodel`), then `previews.py` and `grid.py` for sheets. Each script is a function expression that takes a `cfg` object, see the header comment of each file.

Notes for whoever repeats this:

- The installed GeckoLib plugin (4.2.5) did not finish loading in this Blockbench session: its settings were missing, so creating a GeckoLib project failed until the settings `geckolib_always_show_display`, `geckolib_remember_export_locations`, `geckolib_auto_particle_texture`, `geckolib_default_modid` and `geckolib_bake_in_bezier_keyframes` were registered by hand, and its keyframe patches were inactive, so Blockbench's own animation export dropped the easings. `export.js` therefore writes the animation file straight from the keyframes (Blockbench-native values, `geckolib_format_version: 2`, `easing`, `easingArgs`). The geo file is Blockbench's own Bedrock compile. If the plugin loads fine on your machine, exporting the animations with the plugin's own action should give the same file.
- Blockbench snaps nothing here: keyframe times are written as authored (0.32, 0.46, ...).

# Worn ring rendering (Curios ring slot): spec for the coder

Assets only; no Java was written. A Curios `ICurioRenderer` draws one of these models on the player's arm while the ring is equipped.

## What the ring looks like
Small jewellery at the hand, not a cuff: a **1 px tall, 0.4 px thick silver band at the very bottom of the arm** (the lowest pixel, where the hand is), wrapping the outer face and half of the front and back faces. It is open on the inner side of the arm (against the body). On the outer face (the back of the hand when the arm hangs) sits a **stone of 2 x 1.2 px, raised 0.5 px**, with one highlight pixel. Stones: sun = royal blue lapis with a gold fleck; moon = dark blue-black **kyanite** with a lighter blue streak; resurrection (Gilbert) = black onyx. See `art/previews/items/ring_worn_preview.png` (full body, classic and slim, then close-ups of the six variants).

## Models
`assets/supernaturalbloodlines/models/curio/<ring>[_slim][_left].json` for `sun_ring`, `moon_ring`, `resurrection_ring` (12 files). Textures are `textures/item/3d/curio_<ring>.png` (a 16x16 flat-colour palette shared by the four variants). Blockbench sources: `art/blockbench/items/item_curio_<ring>.bbmodel` (classic, right arm). Rebuild with `py art/items/curio.py`.

| File | Use for |
|---|---|
| `<ring>.json` | right arm, classic (Steve, 4 px wide) |
| `<ring>_left.json` | left arm, classic |
| `<ring>_slim.json` | right arm, slim (Alex, 3 px wide) |
| `<ring>_slim_left.json` | left arm, slim |

Each is a plain block model (no display transforms, no parent) in 0..16 block-model units, **1 unit = 1 pixel of the player model**. It is authored upright (y up) with the ring centre at (8, 8, 8): the band is at y 7.5..8.5, the stone on the **-X** side for the right-arm files and **+X** for the `_left` files, which is the outward side when the arm hangs down. The band is 0.3 px clear of the arm skin, so it sits over the 0.25 px sleeve layer.

## Where it attaches
- **Bone:** the arm `ModelPart` of `PlayerModel`: `rightArm` / `leftArm`. Apply `arm.translateAndRotate(poseStack)` first so the ring follows arm swing, sneaking and use poses. It is not a held item.
- **Which hand:** Curios slot index 0 on the right arm, index 1 on the left. A single ring goes on the right arm.
- **Arm-local pose** (ModelPart space: +Y is down, the front of the player is -Z, pixel units; the arm cube spans y = -2..10, so its **bottom (hand end) is y = 10**):

| Arm | Offset to the ring centre (x, y, z) px | Model file |
|---|---|---|
| Classic right | (-1.0, 9.5, 0) | `<ring>.json` |
| Classic left | (+1.0, 9.5, 0) | `<ring>_left.json` |
| Slim right | (-0.5, 9.5, 0) | `<ring>_slim.json` |
| Slim left | (+0.5, 9.5, 0) | `<ring>_slim_left.json` |

  y = 9.5 puts the band at arm-local y 9..10 (the lowest pixel), with the stone at y 8.8..10. x is the centre of the arm cube (classic cube x = -3..1 right, -1..3 left; slim -2..1 and -1..2).
- **Scale:** 1.0. One model unit is one player pixel.
- **Rotation:** the models are y-up and arm space is y-down, so rotate 180 degrees about X before rendering: `Axis.XP.rotationDegrees(180)`. That keeps the stone on the outer X side and mirrors Z, which only matters for the stone details (they are symmetric enough that nothing needs a fix).
- **Recentre:** the model centre is (8, 8, 8) block-model units, so after the rotation translate by (-0.5, -0.5, -0.5) blocks and render the baked model.

Order for the coder (not code to copy): `armPart.translateAndRotate` -> `translate(ox/16, 9.5/16, 0)` -> `mulPose(XP 180)` -> `translate(-0.5, -0.5, -0.5)` -> render the baked curio model. The preview was built with exactly this transform (the models mirrored in Z, offset (-5 + ox, 12.5) in a y-up player).

## Slim versus classic
Pick by the player's skin model (`PlayerSkin.model() == SLIM`). Slim uses the `_slim*` files, whose band hugs a 3 px arm (outer face 0.5 px closer to the body) and uses the slim offsets above. Z width is the same (4 px).

## Notes
- Hide the ring when the arm is hidden (spectator, invisibility) and when the player model is not the vanilla `PlayerModel`.
- Crouching, swimming and elytra need no special case: the ring is attached to the arm part.
- Do not mirror with a negative scale to get the left hand (it flips face winding); use the `_left` files.
- The held item model (what `sun_ring.json` shows in hands, on the ground and in frames) is a separate small model; see `models3d.py` `RING`.

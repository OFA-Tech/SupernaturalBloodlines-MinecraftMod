# Worn ring rendering (Curios ring slot): spec for the coder

Assets only; no Java was written. A Curios `ICurioRenderer` draws one of these models on the player's arm while the ring is equipped.

## Models
`assets/supernaturalbloodlines/models/curio/<ring>[_slim][_left].json` for `sun_ring`, `moon_ring`, `resurrection_ring` (12 files). Textures are `textures/item/3d/curio_<ring>.png` (a 16x16 flat-colour palette, shared by all four variants). Blockbench sources: `art/blockbench/items/item_curio_<ring>.bbmodel` (classic, right arm). Rebuild with `py art/items/curio.py`.

| File | Use for |
|---|---|
| `<ring>.json` | right arm, classic (Steve, 4 px wide) |
| `<ring>_left.json` | left arm, classic |
| `<ring>_slim.json` | right arm, slim (Alex, 3 px wide) |
| `<ring>_slim_left.json` | left arm, slim |

Each is a plain block model (no display transforms, no parent) in 0..16 block-model units, **1 unit = 1 pixel of the player model**. The ring is built upright: band axis = the arm axis, centre of the band at (8, 8, 8), stone on the **-X** side of the band for the right-arm files and **+X** for the `_left` files, which is the outward side of the hand when the arm hangs down. The band is 0.55 px thick and 1.2 px tall, 0.1 px clear of the 0.25 px sleeve layer, so it sits over sleeves and bare skin without z-fighting. The stone is 0.6 px proud of the band.

## Where it attaches
- **Bone:** the arm `ModelPart` of `PlayerModel`: `rightArm` / `leftArm`. Apply `arm.translateAndRotate(poseStack)` first, so the ring follows arm swing, sneaking and item-use poses. Do not attach to the hand item layer, the ring is not held.
- **Which hand:** slot index 0 on the right arm, index 1 on the left (or the Curios `SlotContext.index()` parity). A single ring goes on the right arm.
- **Arm-local pose** (ModelPart space: +Y is down, +Z is the player's back, pixel units; the arm pivot is at the shoulder; the arm cube spans y = -2..10 so its **bottom (hand end) is y = 10**):

| Arm | Offset to the ring centre (x, y, z) px | Model file |
|---|---|---|
| Classic right | (-1.0, 8.5, 0) | `<ring>.json` |
| Classic left | (+1.0, 8.5, 0) | `<ring>_left.json` |
| Slim right | (-0.5, 8.5, 0) | `<ring>_slim.json` |
| Slim left | (+0.5, 8.5, 0) | `<ring>_slim_left.json` |

  y = 8.5 puts the band's lower edge 0.9 px above the end of the arm, i.e. a ring at the wrist/knuckles end of the hand. x is the centre of the arm cube (classic cube x = -3..1 / -1..3, slim -2..1 / -1..2).
- **Scale:** 1.0. One model unit is one player pixel. Convert to blocks with the usual `1/16`: the pose is already in blocks after `translateAndRotate`, so translate by `offset / 16`, and the model keeps its native 16-unit block space.
- **Rotation:** the models are authored y-up and the arm space is y-down, so rotate 180 degrees about X before rendering: `Axis.XP.rotationDegrees(180)`. This keeps the stone on the outer X side and flips Z only (the models are symmetric in Z except for stone details).
- **Recentre:** the model centre is (8, 8, 8) block-model units, so after the rotation do `translate(-0.5, -0.5, -0.5)` and render the baked model (`ItemRenderer.renderModelLists` / `BlockRenderDispatcher`-style quad render with the item atlas, `LightTexture.FULL_BRIGHT` is not needed).

Pseudo-order (for the coder, not code to copy): `armPart.translateAndRotate` -> `translate(ox/16, 8.5/16, 0)` -> `mulPose(XP 180)` -> `translate(-0.5, -0.5, -0.5)` -> render the baked curio model.

## Slim versus classic
Pick by the player's skin model (`PlayerSkin.model() == SLIM`). Slim uses the `_slim*` files, whose band is 1 px narrower in X (inner half-width 1.85 px against 2.35 px) and whose offset is half a pixel closer to the body (see table). Z width is the same (4 px).

## Notes
- Hide the ring when the arm is hidden (spectator, invisibility) and when the player model is not the vanilla `PlayerModel`.
- Crouching, swimming and elytra need no special case: the ring is attached to the arm part.
- A second ring of the same type on the other hand uses the `_left` file; do not mirror with a negative scale (it flips face winding).
- Preview of the result: `art/previews/items/ring_worn_preview.png` (classic row top, slim row bottom; back three-quarter left, side right; sun, moon, resurrection from left to right in each row, extra arms at the edges are the neighbouring rows).
- Held item model (what `sun_ring.json` shows in hands, on the ground and in frames) is a different, deliberately small model: third/first person transforms put the band around the end of the arm/hand with the stone up; see `models3d.py` `RING`.

# The Beast of Gevaudan: the eight poses and how the frames here were made

All frames in this folder are **Blockbench renders, not the game**: `tick_NN_side.png` (side view, 3.3 screen px per model px), `tick_NN_q.png`
(three-quarter view), `tick_20_front.png` and `contact_sheet.png`. The man is `art/blockbench/bosses/beast_host.bbmodel` posed from
`man_beast_host.json` as the code applies it; **his tricorn and the Valet's coat are separate models and are not in these frames**. The Beast is
`art/blockbench/creatures/beast_of_gevaudan.bbmodel` playing `shift_in` and, at tick 44, `roar`. There is no `shift_out`.

Its rhythm: it looms, then it crashes. The Host is a soldier and moves like one: he stiffens before he breaks.

## The eight poses

| # | Tick | Clip time | Pose to hit |
|---|---|---|---|
| 1 | 6 | man key | He stiffens: chin up 5 degrees, chest out, arms pressed to his sides, 0.3 px lower. No slump. |
| 2 | 12 | man key | **Man peak, the jolt:** he folds 12 degrees, chin driven down 28, and the right claw jerks up to his chest (56 degrees); the left arm has barely moved. |
| 3 | 19 | man key | **Match (man):** head thrown back 38 degrees, right claw at 52 degrees and left at 42, feet nearly together, 0.97 size, top of the head at 32.13 px. |
| 4 | 20 | `shift_in` 0.00 | **Match (first frame):** the Beast at 0.52 of its size, upright on folded legs, 32.42 px (2.03 blocks) tall, claws hooked at the chest, standing where the Host stood. |
| 5 | 26 | `shift_in` 0.30 | Looming: 53.5 px tall, straightened, claws raised and opened out, head back, jaw open. |
| 6 | 32 | `shift_in` 0.60 | **Landing:** the first fist is on the ground (the LAND dust), the other 13.8 px up and down a tick later; both feet back on their spots. |
| 7 | 33 | `shift_in` 0.65 | Impact: the top of it drops 8.1 px in one tick; the mane goes over front to back and the head follows. |
| 8 | 40 | `shift_in` 1.00 | **Settle:** the first frame of `idle` (the model's rest pose), every bone. The roar starts and ends on it. |

The saved frames are the brief's ticks (0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44); poses 1, 2, 5, 7 and 8 fall on ticks between them.

## Measured (Blockbench, every tick of the clip)

* `shift_in`: nothing below the ground at any tick; the fists land at 0.60 and 0.65 s and do not move afterwards (0 px); the feet step back
  (0.30 to 0.48 s and 0.44 to 0.60 s) and do not slide once down (0 px in z). No limb target beyond reach.
  Height goes 32.4, 57.7 (0.45 s), 53.0 (0.60 s), 44.8 (0.65 s), 52.56.
* It ends 40.55 px wide and 52.56 px tall (2.5 by 3.3 blocks): the model's own rest size, which the brief gives as 2.4 by 3.0.

## Where this does not meet the brief, and what is not mine

* **The first frame is 21.1 px (1.32 blocks) wide.** The Host is 16.5. The shoulders cannot be narrower at a size that keeps the height.
* **It stands 3.6 blocks tall at 0.45 s,** 5 px above its own standing height, on the way down. Under a 3-block ceiling it will go through it.
* **The roar's fists go up to 6.75 px into the ground and its feet slide 6 px.** The old roar did this; the re-time keeps its poses (the crouch and
  the burst a little quicker, the held roar 0.79 s instead of 1.1 s), so it keeps this. It needs its own fix.
* **The hat.** The tricorn comes off at the swap in code; I have not seen it, and the first frame was sized against the Host's bare head.

## What I would change (not done)

1. Ticks 25 to 28 (0.25 to 0.40 s) are the loom: tall and nearly still, on purpose. If it reads as a held pose in motion, shorten it.
2. The Host's two claws overlap in the side view at ticks 16 to 19.
3. **None of this has been seen in the game or in motion.**

# Peter Hale: the eight poses and how the frames here were made

All frames in this folder are **Blockbench renders, not the game**: `tick_NN_side.png` (side view, 3.6 screen px per model px), `tick_NN_q.png`
(three-quarter view), `tick_20_front.png` and `contact_sheet.png`. The man is `art/blockbench/bosses/peter_hale.bbmodel` posed from `man_peter.json`
as the code applies it. The wolf is `art/blockbench/creatures/peter_hale_alpha.bbmodel` playing `shift_in` and, at tick 44, `roar`.

His rhythm: slow and sure. A breath in, then he sets himself down on one forepaw and, a tenth of a second later, the other; the weight arrives
late and settles once. Where Klaus falls, Peter lowers himself.

## The eight poses

| # | Tick | Clip time | Pose to hit |
|---|---|---|---|
| 1 | 6 | man key | The shoulders drop: chin down 12 degrees, both arms a little back, 0.7 px lower. |
| 2 | 12 | man key | **Man peak, the neck roll:** head tipped 12 degrees to the side and turned 14, the right claw rising (22 degrees); the left arm has not moved. |
| 3 | 19 | man key | **Match (man):** head back 34 degrees, the left claw (52 degrees) now above the right (42), left leg forward, 0.91 size, top of the head at 29.36 px. |
| 4 | 20 | `shift_in` 0.00 | **Match (wolf first frame):** the alpha at half its size, reared 72 degrees on folded haunches, 29.2 px (1.83 blocks) tall and 11.63 px (0.73) wide, left claw higher, hind feet forward of their rest spots so that he stands where the man stood. |
| 5 | 26 | `shift_in` 0.30 | Growing and tipping; the right hind foot has stepped back to its spot, the forelegs are opening. |
| 6 | 32 | `shift_in` 0.60 | **Landing:** the right forepaw is set down (the LAND dust); the left is 4.9 px up and lands at 0.70 s. |
| 7 | 34 | `shift_in` 0.70 | The weight arrives: the top of him is 3.3 px lower than at contact, the head low, the mane thrown forward. |
| 8 | 40 | `shift_in` 1.00 | **Settle:** the first frame of `idle`, every bone. The roar starts and ends on this same pose. |

The saved frames are the brief's ticks (0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44); poses 1, 2, 5, 7 and 8 fall on ticks between them.

## Measured (Blockbench, every tick of each clip)

* `shift_in`: nothing below the ground at any tick; the right forepaw lands at 0.60 s and the left at 0.70 s, and neither moves afterwards (0 px);
  the hind feet step back (right 0.14 to 0.36 s, left 0.36 to 0.56 s) and do not slide once down (0 px in z). No limb target beyond reach.
  Height goes 29.2, 46.3 (0.40 s), 36.8 (0.70 s), 40.65.
* `shift_out`: nothing below the ground; peak 42.1 px at 0.50 s; it ends exactly on the match pose.

## Where this does not meet the brief, and what is not mine

* **Width at the first frame is 0.73 blocks, not 0.7** (the mane). Height was kept.
* **He stands 2.9 blocks tall at 0.40 s** on the way down: a five-block wolf reared at nearly full size. In smoke at the cut this may be right or too much.
* **The forepaws' lowest cubes are 2.79 px above the ground in the model's own rest pose** (so also in `idle`, `walk` and here once landed). I did not
  change the model; `shift_in` lands the forepaws exactly where `idle` holds them.
* **The roar's paws slide 5 to 7 px and dip up to 1.8 px into the ground.** The old roar did this (it rears the body without solving the legs); the
  re-time keeps its poses, so it keeps this. I added held keys for the two tail bones the old roar never keyed, so the tail does not jump when it starts.

## What I would change (not done)

1. Ticks 20 to 23 are small: a breath, a turn of the head and growth. They may read as held in motion.
2. The head dips close to the ground at tick 35.
3. **None of this has been seen in the game or in motion.**

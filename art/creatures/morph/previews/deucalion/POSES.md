# Deucalion: the eight poses and how the frames here were made

All frames in this folder are **Blockbench renders, not the game**: `tick_NN_side.png` (side view, 4.4 screen px per model px), `tick_NN_q.png`
(three-quarter view), `tick_20_front.png` and `contact_sheet.png`. The man is `art/blockbench/bosses/deucalion_human.bbmodel` posed from
`man_deucalion.json` as the code applies it, with his hat layer (the glasses) hidden, as the game hides it. The wolf is
`art/blockbench/creatures/deucalion_demon_wolf.bbmodel` playing `shift_in` and, at tick 44, `roar`.

His rhythm: one heavy hit and a slow rise. The demon wolf walks upright, so he does not drop onto four paws: he grows into a lunge, drives his left
hand into the ground and stands up to his full three blocks.

## The eight poses

| # | Tick | Clip time | Pose to hit |
|---|---|---|---|
| 1 | 6 | man key | The dip: chin down 10 degrees, hands opening out to the sides, 0.6 px lower. |
| 2 | 12 | man key | **Man peak, the buckle:** he folds forward 14 degrees, chin on his chest, left claw pulled up to the chest (58 degrees); the right arm has barely moved. |
| 3 | 19 | man key | **Match (man):** head rolled back 28 degrees, left claw at 50 degrees and right at 40, 0.93 size, top of the head at 29.67 px. |
| 4 | 20 | `shift_in` 0.00 | **Match (wolf first frame):** the demon wolf at 0.66 of its size in the same hunch, 30.06 px (1.88 blocks) tall, feet where the man's are, left claw higher. |
| 5 | 26 | `shift_in` 0.30 | Growing: the chest has opened, the right foot is stepping back, the left arm reaches out. |
| 6 | 32 | `shift_in` 0.60 | **Landing:** full size, body tipped 73 degrees, the left hand flat on the ground (the LAND dust), the right arm thrown back. |
| 7 | 34 | `shift_in` 0.70 | Impact: the top of him is 5.2 px lower than at contact, the head driven down, hair thrown forward. |
| 8 | 40 | `shift_in` 1.00 | **Settle:** the first frame of `idle`, every bone. The roar starts and ends on this same pose. |

The saved frames are the brief's ticks (0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44); poses 1, 2, 5, 7 and 8 fall on ticks between them.

## Measured (Blockbench, every tick of each clip)

* `shift_in`: nothing below the ground at any tick; the left hand comes down from 3.0 px to the ground at 0.60 s and does not move while it is down
  (0.08 px, 0.60 to 0.70 s); the right foot steps back 0.18 to 0.38 s and up again 0.74 to 0.92 s and does not slide between; the left foot never moves
  (0 px in z). No limb target beyond reach. Height goes 30.1, 36.8 (0.45 s), 27.6 (0.70 s), 49.5.
* `shift_out`: nothing below the ground; neither foot moves; it dips to 25.2 px at 0.65 s (curled over) and ends exactly on the match pose.
* The feet spread 1.4 px sideways over `shift_in` because the whole model grows from 0.66 to 1.

## Where this does not meet the brief

* **Width at the first frame is 17.8 px (1.11 blocks), not 0.7.** His shoulders are that wide at any size that keeps the height; the man himself is 16 px
  wide. Height was kept, width was not.
* **The roar's feet go up to 1.9 px into the ground and slide 4 px** between 0.15 and 0.35 s. The old roar did this too (it dips the hips and swings the
  legs without bending the knees); the re-time keeps its poses, so it keeps this. It needs its own fix.
* **The "landing" is a hand, not forepaws,** because this wolf stands upright. It is on the LAND cue.

## What I would change (not done)

1. The man's two claws overlap in the side view at ticks 15 to 19; from the front they are at different heights, but a side-on player sees one arm.
2. At tick 35 the head is tucked so low that the face is hidden; it may read as a ball.
3. `shift_out` after 0.85 s is small movement only (the head coming up).
4. **None of this has been seen in the game or in motion.**

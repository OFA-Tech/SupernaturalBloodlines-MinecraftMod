# Klaus: the eight poses and how the frames here were made

All frames in this folder are **Blockbench renders, not the game**: `tick_NN_side.png` (side view, 4.5 screen px per model px), `tick_NN_q.png`
(three-quarter view) and `contact_sheet.png` (all twelve ticks; the block lines are drawn on the side rows only, an angled view has no scale).
The man is `art/blockbench/bosses/klaus.bbmodel` posed from `man_klaus.json` exactly as `MorphPlayerModel` and `HumanoidBossRenderer.scale` apply it
(parts turn about their own pivots, the legs do **not** follow the hips, the body is scaled about the feet and moved by `offset_y`). The wolf is
`art/blockbench/creatures/klaus_hybrid.bbmodel` playing `shift_in` and, at tick 44, `howl`.

## The eight poses

| # | Tick | Clip time | Pose to hit |
|---|---|---|---|
| 1 | 6 | man key | The dip: chin down 16 degrees, arms drawn back, 0.7 px lower. Before the first crack. |
| 2 | 12 | man key | **Man peak, the snap:** head thrown back 42 degrees and turned, right arm hooked up 62 degrees, right leg back; the left side has barely moved. |
| 3 | 19 | man key | **Match (man):** both claws up at different heights (48 and 37 degrees), left leg forward, 0.93 size, top of the head at 29.96 px. |
| 4 | 20 | `shift_in` 0.00 | **Match (wolf first frame):** reared on folded haunches, 29.81 px (1.86 blocks) tall, 11.15 px (0.70) wide, right claw higher, left hind foot a step forward. It stands where the man stood: hips over z = 3, both hind feet ahead of their rest spots. |
| 5 | 26 | `shift_in` 0.30 | Tipping: 19 degrees along, hips starting to push, root at 0.89, forelegs reaching; the left hind foot has stepped back to its rest spot and the right is about to. |
| 6 | 32 | `shift_in` 0.60 | **Landing:** left forepaw down (the LAND dust), right 2.1 px above the ground, hind legs at full push, tail streaming up. |
| 7 | 34 | `shift_in` 0.70 | Impact: the top of the wolf is 6.8 px lower than at contact, the head whipping down after the chest, ruff and hackles flared, tail still up. |
| 8 | 40 | `shift_in` 1.00 | **Settle:** the first frame of `idle`, every bone. The howl starts and ends on this same pose. |

The saved frames are the brief's ticks (0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44); poses 1, 2, 5, 7 and 8 fall on ticks between them.

## Measured (Blockbench, every tick of each clip)

* `shift_in`: nothing below the ground at any tick; peak height 35.6 px (2.2 blocks) at 0.40 s; left paw lands at 0.60 s, right at 0.65 s; no paw moves
  after it lands (0 px); no paw target beyond its leg's reach. The body turns 4.8 degrees a tick early in the fall and 8.5 at the ground.
* `shift_out`: nothing below the ground; peak 39.5 px at 0.45 s; right paw leaves at 0.30 s, left at 0.35 s; ends exactly on the match pose.
* `howl` (2.2 s): between its keys the paws dip up to 0.1 px into the ground and drift up to 0.04 px. Not fixed.

## Changed after the owner's first check

* The wolf's first frame stood about 9 px behind the man. Now its hips start over z = 3 with the hind feet 12.5 and 7.5 px forward of their rest spots,
  and the feet step back to those spots during the fall (left 0.10 to 0.32 s, right 0.32 to 0.50 s), so nothing slides. The root bone also carries a
  4.6 px offset in the clip, but the stance is what moves the wolf; the offset alone moves nothing.
* Ticks 20 to 24 were close to one pose: a shudder through the shoulders, the hackles jumping at 0.07 s and the first hind step now fall there.
* The end of `shift_out` was a held pose that shrank: it now walks its hind feet forward under itself (right 0.56 to 0.74 s, left 0.72 to 0.92 s) and shudders.
* All gates above were measured again after these changes and hold with the same limits.

## What I would still change (not done)

1. **The head dips almost to the ground at tick 34.** It may be too much.
2. **The man's feet sink up to 1.6 px** (ticks 16 to 19): the player model has no knees, so the squat is `offset_y`. His lean is only 14 degrees,
   because the code does not carry the legs with the hips.
3. **The howl's paws** dip up to 0.1 px into the ground between its keys.
4. **None of this has been seen in the game or in motion.** `-Dsupernaturalbloodlines.devshots.bosses=morph:klaus` has to be run again with this art.
5. More pain in the man (clutching, buckling) is wanted by the owner and is left for after the first release.

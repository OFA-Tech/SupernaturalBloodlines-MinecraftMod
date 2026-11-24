# Fable brief: UI overhaul phase 7, the morph art

Agent: `anthropic/claude-fable-5-2`, in Blockbench through the MCP (`tools.blockbench` inside `execute`). One boss at a time, in this order: **Klaus, Deucalion, Peter, the Beast** (the Beast only after the first three are accepted by the owner). Our own art only, no image-generation models, no pasted frames from films.

Worktree for the art: `D:\Development\OFA-TECH\.worktrees\SupernaturalBloodlines\ui-p7` (branch `feature/ui-p7`). The full plan is `docs/superpowers/plans/2026-11-23-ui-overhaul-phase7-morphs.md` ("Fable art tasks", A1 to A5, and "Decisions"). Read it first.

## What the code already does (do not change it)

A shift is **40 ticks (2.0 s)**, on the server:

| Ticks | What is drawn | Made by |
|---|---|---|
| 0 to 19 | the **man** (the vanilla player model with the boss's skin), posed from a small table | you: **the man pose table** |
| 20 | the cut: the wolf's body and hitbox, smoke, howl sound | code |
| 20 to 39 | the **wolf** plays `shift_in` (1.0 s) | you: **GeckoLib clip** |
| 40 to 83 | the wolf plays its howl clip (`howl` for Klaus, `roar` for the others), **re-timed to 2.2 s** | you: **re-time** |

Server cues you can rely on (they are code, not art): growl at tick 0, dust at 4, 8, 12, 16 (smoke climbing from the feet to the shoulder), bone cracks at 7 and 14, the swap burst at 20, **the landing dust at tick 32** (so the forepaws touch the ground at 0.6 s into `shift_in`, which is tick 32).

Facing is -Z, left is +X, paws on y = 0. Bone names are those of each `geo.json`. **No model or texture changes.**

Models (source): `art/blockbench/creatures/{klaus_hybrid,deucalion_demon_wolf,peter_hale_alpha,beast_of_gevaudan}.bbmodel`. The men's skeletons for posing: `art/blockbench/bosses/{klaus,deucalion_human,peter_hale,beast_host}`. Game files: `src/main/resources/assets/supernaturalbloodlines/geo/entity/<model>.geo.json` and `.../animations/entity/<model>.animation.json`.

## A1. References first (before any pose)

Write `art/creatures/morph/REFERENCES.md`: what each reference taught, in your words, **no pasted frames**. Film transformations where the cut is hidden by the pose (An American Werewolf in London, 1981: the stretch and the hunch; The Howling, 1981); how this universe shows it (Teen Wolf, The Originals: the bone-break sound, the head thrown back); real wolf stance changes (rising from a crouch, landing on the forepaws); one game that does it well. Output: 8 stills of poses to hit per boss (man peak, wolf first frame, landing, settle), noted as drawings or frame numbers in `art/creatures/morph/previews/<boss>/`.

## A2 to A5. Per boss, deliver

1. **Man pose table** `src/main/resources/assets/supernaturalbloodlines/morph/man_<boss>.json` (`man_klaus`, `man_deucalion`, `man_peter`, `man_beast_host`). The format is fixed by `core/boss/ManPoseTable.java` (read its Javadoc): `{ "keys": [ {"tick":0, ...}, {"tick":6, ...}, {"tick":12, ...}, {"tick":16, ...}, {"tick":19, ...} ], "match": { ... } }`, each key with these 12 channels (degrees for rotations, blocks for `offset_y`, a factor for `scale`; a channel left out is neutral): `head_x`, `head_y`, `head_z`, `body_x`, `right_arm_x`, `right_arm_z`, `left_arm_x`, `left_arm_z`, `right_leg_x`, `left_leg_x`, `offset_y`, `scale`. The code interpolates with smoothstep and only reads the numbers. Limits that a test checks: rotations within 120 degrees, `offset_y` within 0.6 blocks, `scale` 0.7 to 1.1. The files today are PLACEHOLDERS (a mild generic hunch, marked by a `_comment` that you remove).
   - Beats: **0 to 6 anticipation** (the shoulders drop, a small squat); **6 to 16 the hunch** (head back, arms hooked, one leg short), **staggered so that left and right are not mirrored**; **16 to 19 a held breath**, almost still.
   - The `match` block is the pose the wolf's first frame must share (the silhouette at the cut): it equals the last key within 0.01 (`ManPoseTest.lastKeyIsTheMatchPose`).
   - Pose the man's skeleton in Blockbench and **read the numbers off the bones**, do not guess them.
   - Klaus has a snap in his rhythm; Peter's is slow and sure; Deucalion keeps his glasses off (the hat layer is already hidden). The Beast's Host is a soldier in the Valet's coat and tricorn: stiff, not a monster's, and **the hat comes off at the swap**.
2. **`shift_in`**, exactly **1.0 s**, GeckoLib, `loop` false or absent:
   - frame 0 is the `match` silhouette: an upright hunch about **1.75 to 1.9 blocks high and no wider than 0.7** for the three wolves (the Beast's is taller, 2.4 by 3.0 at the end);
   - the spine arches and the forelegs drop from 0.2 s;
   - **the forepaws touch the ground at 0.6 s**, a small bounce, settle;
   - the **last frame equals the first `idle` frame** (within 1 degree and 0.02 blocks, every bone), so the blend into the howl is seamless;
   - the root does not travel more than 0.3 blocks;
   - reuse the poses of Deucalion's `drop_to_fours` and Peter's `rise_up` where they fit, retimed. Smooth with catmullrom on the spine and neck, linear on the legs (the legs are IK-solved in the generator, keep the paws from skating after landing).
3. **The re-timed howl** (`howl` for Klaus, `roar` for Deucalion, Peter, the Beast), exactly **2.2 s**, same poses, shorter holds. Not a speed-up of the whole clip.
4. **`shift_out`**, exactly **1.0 s** (Klaus, Deucalion, Peter; **none for the Beast**). It is authored as ready art and **wired to nothing** in this phase. It is not `shift_in` reversed: the wolf rises off its forepaws, the spine unarches, the head tucks, and it ends on the `match` silhouette.
5. **Export** only the clips above from Blockbench (Export animations) to a temporary file, then run **`python art/creatures/morph/tools/merge_clips.py <model> <exported.json>`** (`--check` first). The tool refuses a wrong length, a loop, an unknown bone, or a missing clip, writes nothing in that case, and keeps every other clip byte for byte. `drop_to_fours` and `rise_up` are deleted by the tool once `shift_in` exists. Commit the `.bbmodel` with the clips in it. Update `art/creatures/morph/README.md` (the generators `klaus.py` and the wolves' library no longer write these clips; the `.bbmodel` is their source from now on).
6. **Review at real size.** Save Blockbench frames at ticks 0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44 to `art/creatures/morph/previews/<boss>/`. Then `./gradlew test --tests "*MorphClipsTest"` passes for the boss. After the shots exist (`-Dsupernaturalbloodlines.devshots.bosses=morph:<boss>`), open the contact sheet and the slow clip and list what you would change.

## Placeholders in place today

`art/creatures/morph/tools/make_placeholders.py` has already written stand-in clips for all four bosses (a held idle pose for `shift_in` and `shift_out`, the old howl stretched to 2.2 s) and mild placeholder pose tables, so the code runs. They are **not meant to look good**; replace all of them.

## What "not AI-ish" means here (the owner checks this)

Poses that are **asymmetric and have weight** (the anticipation dips before the rise); **no linear ease on everything**; **no one-frame pop**; the cut is always inside smoke; the landing has a **visible impact** (a dip in the body, the tail follows a beat late); each boss moves in his **own rhythm**.

## The contract as tests (they pass with the placeholders; they must keep passing as each boss lands)

`MorphClipsTest` (in `src/test/java/.../core/boss/`, plan T5): `shiftInIsOneSecond`, `howlFitsTheRest` (`shift_in` + howl = 3.2 s), `everyBoneExists`, `shiftInEndsInTheIdlePose`, `shiftOutExists` and is 1.0 s (not the Beast), `otherClipsAreUntouched` (a hash of every other animation), `noRootDrift` (the root within 0.3 blocks). `ManPoseTest` (same folder): `lastKeyIsTheMatchPose`, `noChannelJumpsAtAKey`, `theKeysAreAtTheTicksOfThePlan`, `theBodyStaysInsideASensibleRange`, `allFourFilesLoad`. Run both with `./gradlew.bat test --tests "*MorphClipsTest" --tests "*ManPoseTest"` (through `C:\Users\caiom\AppData\Local\Temp\opencode\gt-slot.ps1`, with a timeout). They pass today with the placeholders and must still pass with your art.

## Owner check after each boss, before the next starts

The contact sheet, the slow clip, then the normal-speed clip: the 2 seconds must feel like 2 seconds.

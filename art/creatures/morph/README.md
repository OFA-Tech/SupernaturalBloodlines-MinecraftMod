# Morph art (UI overhaul phase 7)

The transformation of Klaus, Deucalion, Peter Hale and the Beast of Gevaudan: a 2 s shift (`ShiftRules.SHIFT_TICKS = 40`, the body swapped at tick 20).

* `FABLE-BRIEF.md`: what the art agent delivers and the contract (also in `docs/superpowers/plans/2026-11-23-ui-overhaul-phase7-morphs.md`).
* `tools/merge_clips.py`: the **only** way a clip gets into a game animation file. Blockbench's export rewrites a whole file; this takes only `shift_in`, `shift_out` and the re-timed howl (`howl` for Klaus, `roar` for the others) from an export, checks length, loop and bones, and keeps every other clip byte for byte. `test_merge_clips.py` is its test: `python -m unittest art/creatures/morph/tools/test_merge_clips.py` from the repo root.
* `tools/make_placeholders.py`: stand-in clips (a held idle pose, the old howl stretched to 2.2 s), through the merge tool. Replaced by the real art.
* `untouched.sha256`: a hash of every other clip as `main` had it before this phase; `MorphClipsTest.otherClipsAreUntouched` fails when one changes.
* `src/main/resources/assets/supernaturalbloodlines/morph/man_<boss>.json`: the man's pose tables (format: `ManPoseTable`).

* `REFERENCES.md`: what the references taught (A1). `previews/<boss>/`: Blockbench frames at the brief's ticks, a contact sheet and `POSES.md` (the eight
  poses, what was measured, what is still wrong).
* `tools/klaus_keys.py`, `deucalion_keys.py`, `peter_keys.py`, `beast_keys.py`: each boss's `shift_in`, 2.2 s howl or roar and `shift_out` (none for the
  Beast) as a pose sheet. A sheet writes an animation file to **import into the `.bbmodel`**, never the game file: the limbs are IK-solved once a tick
  against the pitching, growing body, which cannot be keyed by hand without the paws skating. A sheet stops if a limb target is more than 0.6 px beyond
  reach, if `shift_in` does not end on idle's first frame or if `shift_out` does not end on the match pose. `morph_rig.py` is the solver the last three
  share (Klaus's sheet has its own, older one). The roars are the old clips from `origin/main` with their key times moved (same poses, shorter holds).

**How the clips got into the game files (2026-11-23).** For all four bosses the game file holds **Blockbench's own GeckoLib export** of the `.bbmodel`,
merged with `merge_clips.py` and compared key by key with the pose sheet (values, key times and easings: no difference). The clips were spliced into each
`.bbmodel` in Blockbench's own serialisation; the model, the texture and every other clip in it are byte for byte what they were. `drop_to_fours`
(Deucalion) and `rise_up` (Peter) are gone from the game files and are still in the `.bbmodel`s as source.

**Easings can be lost on export, and now something notices.** A Blockbench whose GeckoLib plugin is half loaded (its settings missing, opening a GeckoLib
model throws) writes every eased key as linear. That happened here once: Klaus's first howl had to be merged from the pose sheet. `merge_clips.py` now
refuses a clip with no eased key (`--allow-linear` for the held placeholders) and `MorphClipsTest.theClipsKeepTheirEasing` fails on one. Both count only
keys that are eased other than linearly, because a healthy export writes `"easing": "linear"` on every plain key.

**The man's table and the code.** `MorphPlayerModel` turns the body about the neck and does not carry the legs with the hips, so `body_x` above about 14
degrees pulls the torso off the legs; all four tables stay there. The squat is `offset_y`, which sinks the feet (up to 1.9 px). The tables were posed on
the men's models in Blockbench and `offset_y` was solved there; a neutral man is never moved.

**Known and not fixed** (each boss's `previews/<boss>/POSES.md` has the numbers): the three old roars slide their feet and dip them into the ground, and
the re-time keeps that; the first frames of Deucalion and the Beast are wider than the brief's 0.7 blocks; nothing has been seen in the game yet.

The generators `art/creatures/wolves/tools/*.py` (Deucalion, Peter) and `klaus.py` **no longer write** `shift_in`, `shift_out`, the howl clip, `drop_to_fours` or `rise_up`: the `.bbmodel` in `art/blockbench/creatures/` is their source from now on. If a generator is run again anyway, `otherClipsAreUntouched` and `theClipsThisPhaseReplacesAreGone` fail.
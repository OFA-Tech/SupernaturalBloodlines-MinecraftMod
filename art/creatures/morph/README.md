# Morph art (UI overhaul phase 7)

The transformation of Klaus, Deucalion, Peter Hale and the Beast of Gevaudan: a 2 s shift (`ShiftRules.SHIFT_TICKS = 40`, the body swapped at tick 20).

* `FABLE-BRIEF.md`: what the art agent delivers and the contract (also in `docs/superpowers/plans/2026-11-23-ui-overhaul-phase7-morphs.md`).
* `tools/merge_clips.py`: the **only** way a clip gets into a game animation file. Blockbench's export rewrites a whole file; this takes only `shift_in`, `shift_out` and the re-timed howl (`howl` for Klaus, `roar` for the others) from an export, checks length, loop and bones, and keeps every other clip byte for byte. `test_merge_clips.py` is its test: `python -m unittest art/creatures/morph/tools/test_merge_clips.py` from the repo root.
* `tools/make_placeholders.py`: stand-in clips (a held idle pose, the old howl stretched to 2.2 s), through the merge tool. Replaced by the real art.
* `untouched.sha256`: a hash of every other clip as `main` had it before this phase; `MorphClipsTest.otherClipsAreUntouched` fails when one changes.
* `src/main/resources/assets/supernaturalbloodlines/morph/man_<boss>.json`: the man's pose tables (format: `ManPoseTable`).

* `REFERENCES.md`: what the references taught (A1). `previews/<boss>/`: Blockbench frames at the brief's ticks, a contact sheet and `POSES.md` (the eight
  poses, what was measured, what is still wrong).
* `tools/klaus_keys.py`: Klaus's `shift_in`, 2.2 s `howl` and `shift_out` as a pose sheet. It writes an animation file to **import into the `.bbmodel`**, never
  the game file: the legs are IK-solved once a tick against the pitching, growing body, which cannot be keyed by hand without the paws skating. It stops if
  a paw target is more than 0.6 px beyond its leg, if `shift_in` does not end on idle's first frame or if `shift_out` does not end on the match pose.

**Klaus, how his clips got into the game file (2026-11-23).** `shift_in` and `shift_out` are Blockbench's export of the `.bbmodel` (compared key by key with
the pose sheet: 546 keys each, no difference). The `howl` in the game file is the pose sheet's output, **not** Blockbench's export: in that Blockbench
session the GeckoLib plugin was half loaded and its export wrote every eased key as linear (the untouched `attack_bite` lost all 83 of its easings the same
way). The values are identical and the `.bbmodel` holds the easings, so a re-export from a healthy Blockbench should give the same file; check that the howl
still has 80 `"easing"` entries after any re-export, because `merge_clips.py` and `MorphClipsTest` do not look at easings. The three clips were spliced into
the `.bbmodel` in Blockbench's own serialisation; the model, the texture and the other five clips are byte for byte what they were.

**The man's table and the code.** `MorphPlayerModel` turns the body about the neck and does not carry the legs with the hips, so `body_x` above about 14
degrees pulls the torso off the legs; Klaus's table stays there. The squat is `offset_y`, which sinks the feet (up to 1.6 px at tick 19).

The generators `art/creatures/wolves/tools/*.py` (Deucalion, Peter) and `klaus.py` **no longer write** `shift_in`, `shift_out`, the howl clip, `drop_to_fours` or `rise_up`: the `.bbmodel` in `art/blockbench/creatures/` is their source from now on. If a generator is run again anyway, `otherClipsAreUntouched` and `theClipsThisPhaseReplacesAreGone` fail.
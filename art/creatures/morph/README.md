# Morph art (UI overhaul phase 7)

The transformation of Klaus, Deucalion, Peter Hale and the Beast of Gevaudan: a 2 s shift (`ShiftRules.SHIFT_TICKS = 40`, the body swapped at tick 20).

* `FABLE-BRIEF.md`: what the art agent delivers and the contract (also in `docs/superpowers/plans/2026-11-23-ui-overhaul-phase7-morphs.md`).
* `tools/merge_clips.py`: the **only** way a clip gets into a game animation file. Blockbench's export rewrites a whole file; this takes only `shift_in`, `shift_out` and the re-timed howl (`howl` for Klaus, `roar` for the others) from an export, checks length, loop and bones, and keeps every other clip byte for byte. `test_merge_clips.py` is its test: `python -m unittest art/creatures/morph/tools/test_merge_clips.py` from the repo root.
* `tools/make_placeholders.py`: stand-in clips (a held idle pose, the old howl stretched to 2.2 s), through the merge tool. Replaced by the real art.
* `untouched.sha256`: a hash of every other clip as `main` had it before this phase; `MorphClipsTest.otherClipsAreUntouched` fails when one changes.
* `src/main/resources/assets/supernaturalbloodlines/morph/man_<boss>.json`: the man's pose tables (format: `ManPoseTable`).

The generators `art/creatures/wolves/tools/*.py` (Deucalion, Peter) and `klaus.py` **no longer write** `shift_in`, `shift_out`, the howl clip, `drop_to_fours` or `rise_up`: the `.bbmodel` in `art/blockbench/creatures/` is their source from now on. If a generator is run again anyway, `otherClipsAreUntouched` and `theClipsThisPhaseReplacesAreGone` fail.
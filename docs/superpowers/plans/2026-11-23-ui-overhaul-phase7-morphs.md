# UI overhaul phase 7: transformation (morph) animations

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax. TDD: every test must fail when its logic is reverted. Art tasks (A1 to A5) are done by Fable agents in Blockbench through the MCP, model `anthropic/claude-fable-5-2`.

**Goal:** Klaus, Deucalion, Peter Hale and the Beast of Gévaudan change body in a 2 second morph that shows a man bending, a cut at the midpoint, and a wolf arriving, with particles and sound, while the server stays the only authority on form and timing.

**Architecture:** No new rules and no new packets. The synced `FORM` and `SHIFT_TICKS` (already there) drive everything. The client draws a pure function of `(form, shiftTicks, partialTick)`: ticks 0 to 19 the man (player model) in a pose read from a small authored table; tick 20 the body swap (existing `ShiftRenderer`, `ShiftRules.showsBeast`); ticks 20 to 39 the wolf plays a new GeckoLib clip `shift_in`; from 40 the existing howl clip. Server cues (growl, bone cracks, dust, landing) come from a bit mask by tick. The Beast becomes a `ShiftBody` too, so it uses the same code.

**Tech stack:** NeoForge 1.21.1, GeckoLib 4.9.3, JUnit 5, NeoForge GameTest (`runGameTestServer`), Blockbench via MCP, ffmpeg for clips.

**Spec:** `docs/superpowers/specs/2026-11-21-ui-overhaul-design.md` (phase 7 and "Answers to the builder check"); roadmap `docs/superpowers/plans/2026-11-21-ui-overhaul.md` ("Phase 7").

## Global Constraints

Copied from the spec and the owner's answers:

- A transformation takes **2 seconds**: `ShiftRules.SHIFT_TICKS = 40`, body swap at `SWAP_TICK = 20`. Fit the animations to these. The howl stays a separate sound after the swap.
- **The existing shift rules and tests stay valid.** `ShiftRules` and `ShiftRulesTest` are not edited. The boss stays hittable through the whole morph; if any boss is not, fix it and add a test (T3).
- **Reduce motion affects UI only.** Nothing in this phase reads `ReduceMotion`, and no camera shake is added. The boss bar chips already follow it (phase 6).
- Fable makes all art (rule #1135), gathers references first (#1136), and checks each asset at real size in game. Our own art only; references go in a `REFERENCES.md`. No image-generation models.
- The server never loads a client class. No new mixins. No new network payloads.
- Per-part switch: `-PpartsOff=bosses` leaves the game as it was (no renderer, no reload listener, no data).
- Each phase is one PR, `CHANGELOG.md` entry with a "Rolling back" paragraph, panel review, hidden shots (`runSafeShots` under `focus-watch`, never a visible client).

## Review Focus

The input classes the spec does not spell out. Each has a test in the task named after it.

1. **Joining or loading mid-morph** (ticks 1 to 39, also after a save and reload): never the man pose with the wolf hitbox, never invisible, the right body for the tick, and no state that only the client knew. GeckoLib has no seek (see "Route"), so the clip may start late; it must still end in the idle pose at tick 40. Tests: `MorphRulesTest.lateJoin*`, `morphSurvivesSaveAt10And25And39` (T3), the client twin test (T3).
2. **The boss dies, is desiccated or is discarded mid-morph:** no cue after death, the death clip wins, Klaus's counter still waits while desiccated (existing test stays), no stuck beast bonuses. Tests: `noCuesAfterDeath`, `desiccatedKlausPlaysNoCues` (T2).
3. **Hits during the morph** at ticks 0, 19, 20 and 39: each takes health; the swap's hurt tilt (entity event 2) is only visual; a kill during the morph leaves the right form. Test: `hittableAtEveryTickOfTheShift` (T3).
4. **The Beast's edges:** `change()` still makes the Beast at once; a Beast with no morph tags (old save, `/summon`, dev shot) is a full Beast; a collapse during the morph finishes the morph first; the Roar does not start while it changes. Tests in T4.
5. **A client that sees a form it does not know** (modified server, newer save): `BossForm.of` already maps it to HUMAN; the pose table lookup must clamp, never throw. Test: `ManPoseTest.clampsAnyTick`.

## Decisions

### Route (verified with javap on `geckolib-neoforge-1.21.1-4.9.3.jar`; output in `C:\Users\caiom\AppData\Local\Temp\opencode\ui-p7\javap-*.txt`)

Two routes were possible. Chosen: **a match cut at the swap tick between the two renderers that already exist, hidden by particles, with GeckoLib's own animation for the wolf half.** No cross-fade.

What the jar says:

- `AnimationController` blends only between animations of **one** model: `AnimationController(T, String, int transitionTicks, AnimationStateHandler)`, `transitionLength(int)`, `setAnimation(RawAnimation)`, `forceAnimationReset()`, `hasAnimationFinished()`. There is no API that blends two models or two renderers.
- A real cross-fade means drawing both bodies translucent. `GeoRenderer.getRenderColor(T,float,int)` returns a `software.bernie.geckolib.util.Color` record (only `Color.WHITE` and the like are constants, so a per-frame alpha allocates) and `getRenderType(T,ResourceLocation,MultiBufferSource,float)` can return a translucent type. That works but costs two translucent draws, sorting artefacts on overlapping cubes, the glow layer and the player model's second skin layer, and it looks like a generic ghost dissolve. Rejected: it is the "AI-ish" look.
- A match cut is what film does: the last man pose and the first wolf frame share a silhouette, and a 3 tick burst covers the cut. `ShiftRenderer` already picks the body every frame by `ShiftRules.showsBeast`, so there is almost nothing new to render.
- There is **no seek**: `tickOffset` and `adjustTick` are protected. A client that first sees the boss at tick 33 starts `shift_in` from its beginning; the controller leaves it at tick 40 anyway (accepted, Review Focus 1).
- `RawAnimation` is final and has `equals` and `hashCode`, so one static instance per clip is safe and the controller's same-animation check still works. Today each frame calls `RawAnimation.begin()` in all three wolf controllers (an allocation per frame per boss); the new code uses constants.
- `GeoEntity.triggerAnim` sends a packet from the server (Deucalion's `drop_to_fours` uses it). The morph does not: the clip is derived from the synced ticks, so there is nothing to lose or forge.

Not verified here: the Minecraft client classes (`PlayerModel.setupAnim`, `LivingEntityRenderer.scale`), because no merged client jar was found in the Gradle cache; T7 step 1 runs javap on them first. `HumanoidBossRenderer` already overrides `setupRotations(T,PoseStack,float,float,float,float)`, so that signature is known to compile.

### The two halves

| Ticks | Server | Client draws | Made by |
|---|---|---|---|
| 0 to 19 | growl at 0, dust at 4/8/12/16, bone cracks at 7 and 14, he stands still | the man, posed by `ManPose` | Fable: one pose table per boss |
| 20 | swap: wolf hitbox, smoke, howl sound, hurt tilt (all exist) | cut | none |
| 20 to 39 | landing dust at 32 | the wolf plays `shift_in` (1.0 s) | Fable: GeckoLib clip |
| 40 to 83 | the shift is over (`BEAST`), the counter runs on to 84 as today | `howl` / `roar` re-timed to 2.2 s | Fable: re-time |

The man is the vanilla player model with a skin (`HumanoidBossRenderer`), not a GeckoLib model, so his half is a small table of rotations, not a clip. Fable authors it by posing the skeleton in Blockbench; the code only interpolates. The wolf half is a clip because it is a GeckoLib model.

**Why the howl clip is re-timed:** `ShiftRules.howling` is true from tick 20 to 84 (64 ticks, unchanged, tested). `shift_in` takes the first 20 of those, so the howl clip gets the last 44 ticks (2.2 s). Klaus's `howl` is 3.2 s today, Deucalion's, Peter's and the Beast's `roar` are 2.6 s. 1.0 + 2.2 = 3.2 s = `HOWL_TICKS`. No rule changes.

**`shift_out` (decided):** shifts are one way and the Beast leaves through `collapse`, so nothing plays `shift_out`. It is authored for Klaus, Deucalion and Peter as ready art and **wired to nothing** in this phase. T11 is not part of this phase.

**Howl re-time (decided):** yes, the howl clips are re-timed to 2.2 s as above. The owner can overrule both decisions.

### Scope: players who shift (werewolf forms)

**Out of scope, nothing is built.** Reasons:

1. Player werewolf forms are the Werewolves mod's: its form capability, its renderer, its timing. We only cancel or force its `WerewolfFormAction` (`shared/werewolves/ForcedShift`, `FormControl`). We draw nothing for a player's form.
2. Morphing a player means replacing or wrapping another mod's player renderer (a `RenderPlayerEvent` that fights Werewolves' own, or a mixin). The spec says other mods' visuals stay as they are, and a wrapper breaks with each Werewolves update.
3. There is no server clock to be authoritative about: `switchForm` is instant and its form is Werewolves' synced state. The owner's shift rules (`ShiftRules`) are the bosses' rules.
4. The bosses are what the owner named. A morph for players can be a later phase: listen to `ActionEvent.ActionActivatedEvent` for a `WerewolfFormAction` and play only our particles and sound at the player (no model change), behind `isLoaded("werewolves")`.

`docs/parts/bosses.md` gets one line saying so (T10).

### The boss bar

Phase 6 (`feature/ui-p6`, not on main when this was written) already sends the form: `BossBarForms.shifter(form)` for Klaus, Deucalion and Peter (chip 0 Human, 1 Shifting, 2 Beast) and `BossBarForms.host(stage)` for the Beast. `BossNpc.tick` pushes the bar the same tick `barLook()` changes. So the chip goes to **Shifting at tick 0 and Beast at tick 40**, and the gold look means "can be hit". This phase adds no payload field. The Beast's `barLook()` becomes: Host (0) while the man is drawn (ticks 0 to 19), Beast (1) from the swap, Spirit (2) when it collapses.

**Dependency (decided):** T4 and the bar tests need phase 6. Phase 7 is built after PR #73 (phase 6) merges, from `main`. It is not stacked on `feature/ui-p6`.

## Files

Create:
- `src/main/java/net/ofatech/supernaturalbloodlines/core/boss/MorphRules.java`: pure cues and phases.
- `src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/entity/MorphCues.java`: server particles and sounds by cue bit; test observer.
- `src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/client/ManPose.java`: table, loader, sampler.
- `src/main/java/net/ofatech/supernaturalbloodlines/parts/bosses/client/MorphPlayerModel.java`: `PlayerModel` that applies the pose.
- `src/main/resources/assets/supernaturalbloodlines/morph/man_klaus.json`, `man_deucalion.json`, `man_peter.json`, `man_beast_host.json`.
- `art/creatures/morph/` : `README.md`, `REFERENCES.md`, `tools/merge_clips.py`, `tools/make_clip.ps1`, `previews/`.
- Tests: `core/boss/MorphRulesTest`, `core/boss/MorphClipsTest`, `parts/bosses/client/ManPoseTest`, `parts/bosses/MorphNoClientImportsTest`, `parts/bosses/gametest/MorphGameTests`.

Modify:
- `core/boss/KlausRules.java` (one more `animation` overload), `parts/bosses/entity/ShiftCore.java`, `ShiftBody.java`, `KlausEntity.java`, `DeucalionEntity.java`, `PeterHaleEntity.java`, `BeastEntity.java`, `parts/bosses/director/BeastDirector.java`.
- `parts/bosses/client/ShiftRenderer.java`, `HumanoidBossRenderer.java`, `BossRenderers.java`, `BossShots.java`; delete `client/BeastRenderer.java` (replaced by `ShiftRenderer`).
- `assets/supernaturalbloodlines/animations/entity/{klaus_hybrid,deucalion_demon_wolf,peter_hale_alpha,beast_of_gevaudan}.animation.json` (through the merge tool only).
- `art/blockbench/creatures/{klaus_hybrid,deucalion_demon_wolf,peter_hale_alpha,beast_of_gevaudan}.bbmodel`.
- `docs/parts/bosses.md` (R161), `CHANGELOG.md`.

## Tasks

### T1: Pure rules (`MorphRules`, animation choice)

**Interfaces. Produces:**
`MorphRules.GROWL=1, DUST=2, CRACK=4, LAND=8` (int bits);
`int cues(BossForm form, int tick)`: 0 unless `form == SHIFTING`; GROWL at 0; DUST at 4, 8, 12, 16; CRACK at 7, 14; LAND at `SWAP_TICK + 12`;
`boolean manHalf(BossForm form, int ticks)`: SHIFTING and `ticks < SWAP_TICK`;
`boolean arriving(BossForm form, int ticks)`: SHIFTING and `SWAP_TICK <= ticks < SHIFT_TICKS`;
`float manProgress(int ticks, float partial)`: 0 to 1 over `[0, SWAP_TICK]`, `ticks + min(partial, 1)`, clamped;
`KlausRules.animation(boolean deadOrDying, boolean desiccated, boolean arriving, boolean howling, boolean moving, double speed)`: `"shift_in"` after death and before howl; the old five-argument method stays and calls it with `false`.

- [ ] **Step 1: Write the failing tests** in `MorphRulesTest`: `cuesFireOnceAtTheirTicks` (sum over ticks 0 to 83 of `bitCount(cues(SHIFTING, t))` is 8; none for HUMAN or BEAST); `halvesMeetAtTheSwap` (`manHalf` is true at 19 and false at 20, `arriving` the other way round, `arriving` false at 40, and `manHalf(SHIFTING,t) == !ShiftRules.showsBeast(SHIFTING,t)` for every t from 0 to 39); `lateJoinAnyTick` (for every form and tick 0 to 200 exactly one of man, arriving, wolf-after holds, no exception); `manProgressIsMonotoneAndCapped` (0 at tick 0, 1 at tick 19.99 and beyond, never above 1 with `partial = 5`); `deathBeatsArrival`; `arrivalBeatsHowl` (ticks 20 to 39 give `shift_in` although `ShiftRules.howling` is true; tick 40 gives `howl`); existing `KlausRulesTest` expectations unchanged.
- [ ] **Step 2: Run** `./gradlew.bat test --tests "*MorphRulesTest" --tests "*KlausRulesTest"`: FAIL (not defined).
- [ ] **Step 3: Implement** both in `core/boss` with no Minecraft imports.
- [ ] **Step 4: Run** the same: PASS. `ShiftRulesTest` PASS unedited.
- [ ] **Step 5: Commit** `feat(boss): morph rules and the arrival clip choice`.

### T2: Server cues

**Interfaces. Consumes** `MorphRules.cues`. **Produces:** `ShiftBody` is unchanged; `ShiftCore.advance(ShiftBody)` calls `MorphCues.play(ServerLevel, Entity, int cues)` after counting (every ShiftBody is an entity); `MorphCues.observe(@Nullable IntConsumer)` for tests (same pattern as `KlausEntity.observeSwap`).

Cues, vanilla assets only: GROWL `WOLF_GROWL` pitch 0.6 volume 1.0 (`SoundSource.HOSTILE`); CRACK `BONE_BLOCK_BREAK` pitch 0.7 volume 0.8; DUST 4 `ParticleTypes.SMOKE` rising from the feet to the shoulder (`sendParticles` count 4, spread 0.25, speed 0.01); LAND 12 `POOF` at the feet, spread 0.4, plus `WOLF_STEP` pitch 0.5. The swap burst stays where it is (`onSwap`). No cue after `isDeadOrDying()`.

- [ ] **Step 1: Write the failing GameTests** in `MorphGameTests`, one set per boss through `BossTestRig`: `cuesRunOnTheirTicks` (observer sees bits 1 at tick 0, 2 at 4, 8, 12, 16, 4 at 7 and 14, 8 at 32, nothing else), `noCuesAfterDeath` (kill at tick 10: nothing at 14), `desiccatedKlausPlaysNoCues`, `aShiftTwiceInOneLifeIsImpossible` (`startShift` on a SHIFTING or BEAST boss changes nothing, no new cue).
- [ ] **Step 2: Run** `./gradlew.bat runGameTestServer`: FAIL.
- [ ] **Step 3: Implement** `MorphCues` and the hook in `ShiftCore.advance` (guard on `isDeadOrDying`).
- [ ] **Step 4: Run** the matrix run 1: PASS.
- [ ] **Step 5: Commit** `feat(boss): cues for the first half of the shift`.

### T3: The hit window, the standing still, the sync

**Files:** `MorphGameTests`; fixes only if a test fails.

- [ ] **Step 1: Write the tests** for Klaus, Deucalion and Peter:
  - `hittableAtEveryTickOfTheShift`: at ticks 0, 19, 20 and 39, set `invulnerableTime = 0`, hurt with `playerAttack`, assert health fell by the damage (ATTACK is not the only way: also `magic`). Today nothing sets invulnerability in these classes, so this should pass; it pins it.
  - `standsStillForTheWholeShift`: position equal at ticks 0 and 39 (`ShiftCore.hold`).
  - `clientTwinSeesTheSameBody`: copy `entityData.packAll()` into a second entity of the same type with `assignValues` each tick from 0 to 40 and assert `form()`, `shiftTicks()`, `ShiftRules.showsBeast` and the dimensions match the server entity (this is the codec the client uses; it proves nothing but synced data decides what is drawn).
  - `morphSurvivesSaveAt10And25And39`: save and load an entity at each tick, assert form and ticks, and that the swap still happens exactly once.
  - `theBarShowsTheForm` (needs phase 6): `BossBars.sentOf(uuid).look()` is Shifting at tick 0 and Beast at tick 40, on the tick of the change, for each of the three.
- [ ] **Step 2: Run** run 1. Expected: PASS for the first four; if a boss is invulnerable, fix the cause (not the test) and say so in the PR.
- [ ] **Step 3: Commit** `test(boss): the shift is hittable, still and synced`.

### T4: The Beast changes like the others

**Interfaces. Produces:** `BeastEntity implements GeoEntity, ShiftBody`: synced `FORM` (default `BEAST`) and `SHIFT_TICKS` (default `ShiftRules.END_TICK`), `beginMorph()` (form `SHIFTING`, ticks 0), `skin()` returns `"sebastien_valet"`, `BeastRules.dimensionsFor(BossForm, int)` (man 0.6 by 1.95 until the swap, then 2.4 by 3.0), `barLook()` as in "The boss bar". `BeastDirector.change` keeps its signature and result.

- [ ] **Step 1: Write the failing GameTests** (`BeastGameTests` and `MorphGameTests`): `changeStillMakesTheBeastAtOnce` (existing assertions: Beast present, Host gone, phase BEAST, index adopted), `theBeastIsAManUntilTheSwap` (form SHIFTING, hitbox 0.6 by 1.95 at tick 19, 2.4 by 3.0 at tick 20), `theBeastHoldsStill`, `noRoarWhileItChanges` (`RoarGoal.canUse` false for ticks 0 to 39), `theBeastIsHittableWhileItChanges`, `aBeastWithNoMorphTagsIsWhole` (old save: form BEAST, ticks END, full size), `collapseFinishesTheMorph`, `theHostsHatComesOffAtTheSwap` (HEAD copied from the Host at change, empty after tick 20), `barChipsHostThenBeast` (0 until tick 19, 1 from 20, 2 when collapsing), `savedMidChangeResumes`.
- [ ] **Step 2: Run** run 1: FAIL.
- [ ] **Step 3: Implement.** `BeastEntity` follows `KlausEntity` (synced data, `onSyncedDataUpdated` refreshes dimensions, `tick` calls `ShiftCore.hold` before and `ShiftCore.advance` after, not while collapsing). `onSwap` takes over what `BeastDirector.change` did at once: 60 `LARGE_SMOKE`, `WOLF_HOWL` 4.0 / 0.3, clearing the hat. `BeastDirector.change`: copy the Host's HEAD, `beginMorph()`, keep the message, drop the smoke, sound and `beast.howl()` (and delete `howl()` and its `HOWL` trigger; the clip comes from the state now). `collapse()` sets BEAST and END first.
- [ ] **Step 4: Run** matrix runs 1 and 11 (`bosses` off: nothing loads): PASS.
- [ ] **Step 5: Commit** `feat(beast): the Host changes in a shift of its own`.

### T5: The art contract as tests (written before the art)

`MorphClipsTest` reads the four `animation.json` files and the four `geo.json` files with Gson (no Minecraft).

- [ ] **Step 1: Write the failing tests:** `shiftInIsOneSecond` (`animation_length` is `(SHIFT_TICKS - SWAP_TICK) / 20.0` and `loop` is false or absent); `howlFitsTheRest` (`shift_in + howl` equals `HOWL_TICKS / 20.0`; for Deucalion, Peter and the Beast the clip is `roar`); `everyBoneExists` (each bone named in `shift_in`, `shift_out` and the re-timed clip is a bone of the model's geo); `shiftInEndsInTheIdlePose` (for every bone, the last `shift_in` keyframe equals the first `idle` keyframe within 1 degree and 0.02 blocks, so the blend into the howl is seamless); `shiftOutExists` and is 1.0 s (Klaus, Deucalion, Peter only; the Beast has none); `otherClipsAreUntouched` (a hash of every other animation, taken from `main` before the art lands and stored in `art/creatures/morph/untouched.sha256`); `noRootDrift` (the root bone's position in `shift_in` stays within 0.3 blocks, because the entity does not move).
- [ ] **Step 2: Run** `./gradlew.bat test --tests "*MorphClipsTest"`: FAIL. They pass one boss at a time as A2 to A5 land.
- [ ] **Step 3: Commit** `test(boss): what a shift clip must satisfy`.

### T6: Art tooling

**Files:** `art/creatures/morph/tools/merge_clips.py`.

Blockbench's export rewrites the whole animation file, and `art/creatures/*/tools/*.py` generators also write it. To keep the other clips byte for byte, Fable exports only the clips it made and the tool merges them into the game file by name.

- [ ] **Step 1:** `merge_clips.py <model> <exported.json>`: replaces `shift_in`, `shift_out` and the re-timed clip, deletes `drop_to_fours` (Deucalion) and `rise_up` (Peter) once `shift_in` exists, keeps every other key and its order; fails if an animation name or bone is unknown.
- [ ] **Step 2:** `README.md` says the generators (`klaus.py` and the wolves' library) no longer write these clips, and that the `.bbmodel` is their source from now on; `otherClipsAreUntouched` is the guard if someone regenerates.
- [ ] **Step 3: Commit** `chore(art): merge tool for the morph clips`.

### T7: Client: the man's pose, the arrival clip

**Interfaces. Produces:**
`ManPose.load(ResourceManager)` (reload listener, parsed once into `float[][]` per boss id), `ManPose.sample(String bossId, float progress, float[] out)` writing 12 channels (head x y z, body x, right arm x z, left arm x z, right leg x, left leg x, offset y, scale) with smoothstep between keys at ticks 0, 6, 12, 16, 19, clamping any input;
`MorphPlayerModel<T> extends PlayerModel<T>` with `float morph = -1` (set by the renderer; -1 means none); `setupAnim` calls super, then applies the channels;
the three wolf controllers and the Beast's use static `RawAnimation` constants and `MorphRules.arriving`.

- [ ] **Step 1: javap** `PlayerModel.setupAnim` and `LivingEntityRenderer.scale` from the NeoForge client jar; write the signatures into the PR.
- [ ] **Step 2: Write the failing tests** `ManPoseTest`: `clampsAnyTick` (NaN, negative, 99 give finite values), `lastKeyIsTheMatchPose` (the file's `match` block equals the last key within 0.01), `noChannelJumpsAtAKey` (continuity), `allFourFilesLoad`, `sampleAllocatesNothing` (call 10 000 times and check the same `out` array).
- [ ] **Step 3: Implement.** `HumanoidBossRenderer.render`: if the boss is a `ShiftBody` and `MorphRules.manHalf`, set `model.morph = MorphRules.manProgress(ticks, partialTick)`; else -1. Override `scale(T, PoseStack, float)` for offset and size from the same sample (read from a field written by `setupAnim`, not recomputed). `ShiftRenderer` is unchanged except it now also takes the Beast; `BossRenderers`: the Beast uses `new ShiftRenderer<>(context, "beast_of_gevaudan", "beast_of_gevaudan_geo", 1.4F)`; delete `BeastRenderer`. Entities: replace the `RawAnimation.begin()` per frame with `private static final RawAnimation` constants, add `shift_in`, remove Deucalion's `DROP` trigger.
- [ ] **Step 4: Run** `./gradlew.bat test` and `compileJava`: PASS. The pose table files are placeholders until A2 to A5 (a neutral table: all zero), so this task does not wait for art.
- [ ] **Step 5: Commit** `feat(boss): the man's half of the shift and the wolf's arrival`.

### T8: Server and client stay apart

- [ ] **Step 1: Write the failing test** `MorphNoClientImportsTest` (a source scan, no Minecraft): no file under `core/boss`, `parts/bosses/entity` or `parts/bosses/director` has an import starting `net.minecraft.client`, `com.mojang.blaze3d`, `net.neoforged.neoforge.client`, `software.bernie.geckolib.renderer` or `software.bernie.geckolib.model`; and no morph file (`MorphRules`, `MorphCues`, `ShiftCore`, the four entities, `ManPose`, `MorphPlayerModel`, `HumanoidBossRenderer`, `ShiftRenderer`) names `ReduceMotion`. Add a mutation check: the test fails when it is pointed at `parts/bosses/client`.
- [ ] **Step 2: Run:** PASS today (verified: only `parts/bosses/client` imports them); keep it so.
- [ ] **Step 3: Commit** `test(boss): no client classes on the server, no reduce-motion in the world`.

### T9: Hidden shots and clips

**Files:** `BossShots.java`, `art/creatures/morph/tools/make_clip.ps1`.

- [ ] **Step 1: Shots.** New specs `-Dsupernaturalbloodlines.devshots.bosses=morph:klaus|deucalion|peter|beast`. A flat world, the boss spawned 4 blocks from a fixed camera (front three quarter, 1920 by 1080, hidden window), the player's real boss bar visible. The driver freezes the server (`/tick freeze`), sets the boss to start shifting and steps `/tick step 1`, grabbing a frame at ticks 0, 6, 12, 19, 20, 26, 32, 39, 44 into `bossshots/morph-<boss>-t<tick>.png`, then a contact sheet `morph-<boss>-sheet.png`. Assumption to check first: with the tick frozen the client's `tickCount` and GeckoLib's clock stand still too. If they do not, fall back to the clip frames below and take the stills from them by frame number.
- [ ] **Step 2: Clips.** The same scene at `maxFps 30`, one `Screenshot.grab` per rendered frame for 3.5 s into `bossshots/morph-<boss>/%04d.png`; `make_clip.ps1` runs `ffmpeg -framerate 30 -i %04d.png -c:v libx264 -pix_fmt yuv420p -crf 18 morph-<boss>.mp4` and a half-speed `morph-<boss>-slow.mp4` (`-vf setpts=2*PTS`). The log lists frame count (about 105) and any dropped frame; a gap fails the run.
- [ ] **Step 3: Run** hidden under `focus-watch`, logs and the focus CSV in `C:\Users\caiom\AppData\Local\Temp\opencode\ui-p7\`. Copy the sheets and the four mp4 (under 5 MB each) into `docs/superpowers/design/bosses/morph/`.
- [ ] **Step 4: Review** each sheet at real size: the silhouette at tick 19 matches tick 20, the cut is covered by smoke, nothing pops at tick 40, the bar chip is right. Also Klaus with the bar at GUI scales 2 and 4.
- [ ] **Step 5: Commit** `feat(boss): dev shots and clips of the morphs`.

### T10: Docs

- [ ] `docs/parts/bosses.md`: R161 gets the new text (the swap at 20 is the cut; the arrival runs 20 to 39; the howl clip runs 40 to 83; the cues), a line that the Beast is a shifter now, and a line that player werewolf forms are the Werewolves mod's. `CHANGELOG.md`: entry and "Rolling back" (revert the PR; saves are fine because the tags are optional).
- [ ] Commit `docs(boss): the morph rules`.

### T11 (declined for this phase, kept as a note): `shift_out` at dawn

At dawn or when the hunt is lost the wolf plays `shift_out` and a puff, then goes. `KlausEntity.withdraw` and the same in Deucalion and Peter wait 20 ticks (saved counter, still hittable, still tracked) before `hunt.leave` and `discard()`. Tests: `leavesAfterTheClip`, `diesDuringTheLeave`, `savedWhileLeaving`. Not built unless the owner asks later.

## Fable art tasks

Agent: `anthropic/claude-fable-5-2`, working in Blockbench through the MCP (`tools.blockbench` inside `execute`). One agent per boss, run in order A1 to A5. Models are in `art/blockbench/creatures/` (four) and the men's skeletons in `art/blockbench/bosses/` (`klaus`, `deucalion_human`, `peter_hale`, `beast_host`). Facing is -Z, left is +X, paws on y=0, bone names as in each `geo.json` (Klaus: 27 bones, `root > body > neck > head ...`). **No model or texture changes.**

**A1: References (before any pose).** Collect and note in `art/creatures/morph/REFERENCES.md` (what each taught, no pasted frames): film transformations where the cut is hidden by the pose (An American Werewolf in London, 1981: the stretch and the hunch; The Howling, 1981), how this universe shows it (Teen Wolf, The Originals: the bone-break sound, the head thrown back), real wolf stance changes (rising from a crouch, landing on the forepaws), and one game that does it well. Output: 8 stills of poses to hit (man peak, wolf first frame, landing, settle) per boss.

**A2 to A5: Klaus, Deucalion, Peter, the Beast** (Beast after A2 to A4 are accepted). Each agent delivers, per boss:

1. **Man pose table** `assets/.../morph/man_<boss>.json`: keys at ticks 0, 6, 12, 16, 19 and a `match` block. Beats: 0 to 6 anticipation (shoulders drop, a small squat); 6 to 16 the hunch (head back, arms hooked, one leg short), staggered so the left and right sides are not mirrored; 16 to 19 a held breath, almost still. Posed on the man's skeleton in Blockbench; the numbers are read off the bones, not guessed. Deucalion keeps the glasses off (hat layer is already hidden); Peter and Klaus differ in rhythm (Peter's is slow and sure, Klaus's has a snap), not only in numbers. The Beast's Host in the Valet's coat and tricorn: the pose is a stiff soldier's, not a monster's, and the hat comes off at the swap.
2. **`shift_in`**, exactly 1.0 s: frame 0 is the `match` silhouette (an upright hunch about 1.75 to 1.9 blocks high and no wider than 0.7 for the three wolves; the Beast's is taller, 2.4 by 3.0 at the end); the spine arches and the forelegs drop from 0.2 s; **forepaws touch the ground at 0.6 s** (the LAND cue is on tick 32), a small bounce, settle; the last frame equals the first `idle` frame (`shiftInEndsInTheIdlePose`). The root does not travel more than 0.3 blocks. Reuse the poses of Deucalion's `drop_to_fours` and Peter's `rise_up` where they fit, retimed. Smooth with catmullrom on the spine and neck, linear on the legs (legs are IK-solved in the generator, so keep the paws from skating after landing).
3. **The re-timed howl** (`howl` for Klaus, `roar` for the others), exactly 2.2 s, same poses, shorter holds. Not a speed-up of the whole clip.
4. **`shift_out`**, exactly 1.0 s (Klaus, Deucalion, Peter; the Beast's exit is `collapse`, so none for it unless the owner asks). It is not `shift_in` reversed: it anticipates differently (the wolf rises off its forepaws, the spine unarches, the head tucks, ending on the `match` silhouette).
5. **Export** only the clips above and run `merge_clips.py`; commit the `.bbmodel` with the clips in it. Update `art/creatures/morph/README.md`.
6. **Review at real size:** Blockbench frames at ticks 0, 5, 10, 15, 19, 20, 25, 30, 32, 35, 39, 44 are saved to `art/creatures/morph/previews/<boss>/`. Then `MorphClipsTest` passes for the boss, and after T9 the agent opens the contact sheet and the slow clip and lists what it would change. **Owner check** per boss before the next one starts.

What "not AI-ish" means here, to check in review: poses that are asymmetric and have weight (the anticipation dips before the rise); no linear ease on everything; no one-frame pop; the cut always inside smoke; the landing has a visible impact (a dip in the body, the tail follows a beat late); each boss moves in his own rhythm.

## Performance

- Client: `ManPose.sample` writes into a reused `float[12]`; the table is parsed once on resource reload. No `Color`, `Vector`, `RawAnimation` or lambda is created per frame: the clips are static constants, the pose goes through fields. The pose and scale are read once per render. Two renderers already exist; only one draws at a time. No translucent pass.
- Server: cues are an `int` mask by tick; at most one `sendParticles` per cue tick (4 to 12 particles; the swap burst of 60 is unchanged). No scan, no lambda, nothing per tick except the existing counter. No packet is added: `SHIFT_TICKS` is the one int that already changes each tick for 84 ticks. The Beast adds two data entries that never change after its morph.
- Verification: a unit test that calls `ManPose.sample` 10 000 times and a look at the JFR allocation of one morph in the clip run (no growth beyond the frame's own).

## Dedicated-server safety

- All new server code is in `core/boss` and `parts/bosses/entity`, importing no client class (`MorphNoClientImportsTest`). GeckoLib's animation types (`RawAnimation`, `AnimationController`) are common code and are already used by the entities.
- Client classes (`ManPose`, `MorphPlayerModel`, `HumanoidBossRenderer`, `ShiftRenderer`, `BossShots`) stay in `parts/bosses/client`, registered only through the client entry that `BossRenderers.register` already uses; the reload listener goes there too.
- `runGameTestServer` is a dedicated server: any leaked client class fails it with `NoClassDefFoundError`. All 12 runs below catch it.
- The client never advances or decides the form (it reads synced data); a modified client can only change how it looks to itself.

## Test matrix (before the PR, one run at a time, `--max-workers=2`)

`./gradlew.bat test`, then:

1. `./gradlew runGameTestServer`
2. `./gradlew runGameTestServer -PnoWerewolves -PnoCurios`
3. `./gradlew runGameTestServer -PnoLinkedMods`
4. `./gradlew runGameTestServer -PpartsOff=species`
5. `./gradlew runGameTestServer -PpartsOff=artifacts`
6. `./gradlew runGameTestServer -PpartsOff=speciesTensura`
7. `./gradlew runGameTestServer -PpartsOff=supernatural`
8. `./gradlew runGameTestServer -PpartsOff=supernaturalTensura`
9. `./gradlew runGameTestServer -PpartsOff=hud`
10. `./gradlew runGameTestServer -PpartsOff=celestials`
11. `./gradlew runGameTestServer -PpartsOff=bosses` (the morph classes do not load; `MorphGameTests` skips, not errors)
12. `./gradlew runGameTestServer -PpartsOff=species,artifacts`

Expected: green or clean skip in all 12. Runs 2 and 3 (no Werewolves, no linked mods): Klaus, Deucalion and Peter are Werewolves-linked in some tests; the morph tests use the rig and must not need the mod. Logs of each run go to `C:\Users\caiom\AppData\Local\Temp\opencode\ui-p7\`.

## Owner checks and done

- Per boss: the contact sheet and the slow clip, then the normal-speed clip (the 2 s must feel like 2 s).
- Gate (from the roadmap): existing shift tests green, the hit window tested, a reviewed clip per boss, panel review, matrix green, CHANGELOG and docs done.

## Self-review

- **Spec coverage:** 2 s and swap at the midpoint (T1, T5, A-tasks); existing rules untouched (Constraints, T1 step 4); hittable (T3, T4); reduce motion (T8); bar form (T3, T4, "The boss bar"); Fable briefs, references, real-size review, owner check (A1 to A5); clips (T9); server timing authoritative (Architecture, T3 twin test); players decided (Scope).
- **Names:** `MorphRules`, `MorphCues`, `ManPose`, `MorphPlayerModel`, `KlausRules.animation` overload, `BeastRules.dimensionsFor` are the same wherever they appear.
- **Left open on purpose:** exact pose numbers and clip keyframes (the art), per-boss variation of sound and particles (one set of cues for all four for now).

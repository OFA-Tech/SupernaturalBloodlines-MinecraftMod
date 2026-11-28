# Boss Transformation Animations — Design Doc

> **Status:** DESIGN ONLY — no code, no Blockbench files, no art assets.
> **Branch:** `feature/art-boss-transformations-design`
> **Owner request:** Transformations need more "pain." Currently 40 ticks (2 s) with a clean body swap at tick 20 feels too smooth.
> **Scope:** Post-release art work. This doc is the spec for when the pain rework is un-parked.

---

## 1. Current State

### What ships today

| System | File | Behaviour |
|--------|------|-----------|
| Shift timing | `ShiftRules` | 40 ticks total; body swap at tick 20 (halfway); howl plays ticks 20–83 |
| Cue schedule | `MorphRules.cues()` | Growl at tick 0; dust at 4/8/12/16; bone crack at 7/14; landing poof at tick 32 |
| Cue playback | `MorphCues.play()` | Vanilla sounds (`WOLF_GROWL`, `BONE_BLOCK_BREAK`, `WOLF_STEP`) and particles (`SMOKE`, `POOF`) |
| Renderer | `ShiftRenderer` | Swaps between `HumanoidBossRenderer` (man) and `BeastRenderer` (GeckoLib wolf) at tick 20; zero-blend on arrival to avoid pop |
| Body interface | `ShiftBody` | `form()`, `shiftTicks()`, `onSwap()` — implemented by Klaus, Deucalion, Beast, Peter Hale |
| GeckoLib models | `geo/entity/` | `klaus_hybrid`, `deucalion_demon_wolf`, `beast_of_gevaudan`, `peter_hale_alpha` |
| Existing clips | `ShiftCore.clips()` | `death`, `howl`/`roar`, `shift_in`, `idle`, `walk`, `run` per model |
| Morph videos | `docs/superpowers/design/bosses/morph/` | Reference recordings for Klaus, Deucalion, Beast, Peter Hale (normal + slow-mo + contact sheets) |

### What feels wrong

- The man-to-wolf cut at tick 20 is a hard model swap with no intermediate frames.
- Bone cracks are two sound effects at fixed ticks — no visual counterpart on the man's body.
- Dust particles climb linearly but don't follow the skeleton or convey contortion.
- No screen shake, no distortion, no camera work.
- All four shifting bosses share identical timing and cues — no personality.
- The five non-shifting bosses (Mikael, Elijah, Kol, Rebekah, Finn) have no transformation at all.

---

## 2. Design Goals

1. **Pain is visible.** The player should feel the boss is suffering, not switching costumes.
2. **Each boss is distinct.** Klaus's hybrid shift is body horror; the Beast's is spiritual; Deucalion's is demonic emergence; Peter's is feral rage.
3. **The existing architecture holds.** `ShiftBody`, `ShiftCore`, `ShiftRenderer`, `MorphRules`, `MorphCues` stay; new behaviour plugs into the cue system and GeckoLib controllers.
4. **Accessibility first.** Every painful effect has a reduce-motion fallback that preserves readability.
5. **Art effort is bounded.** Reuse existing models and clips where possible; new assets are intermediate poses and particle textures, not new models.

---

## 3. Per-Boss Transformation Sequences

### Timing framework

All bosses keep the 40-tick total and tick-20 swap. Pain comes from *what happens within those ticks*, not from making them longer (longer shifts hurt combat pacing). The cue bitmask in `MorphRules` expands to carry boss-specific events; `MorphCues` dispatches them.

New cue bits (additive, backwards-compatible):

| Bit | Name | Purpose |
|-----|------|---------|
| 16 | SPASM | Man-body spasm pose (client bone override) |
| 32 | DISTORT | Screen distortion pulse (client shader uniform) |
| 64 | SHAKE | Camera shake impulse (client) |
| 128 | BLOOD | Blood particle burst (server) |
| 256 | GLOW_PULSE | Emissive texture intensity spike (client) |
| 512 | SPIRITUAL | Spirit wisp particles (server, Beast only) |
| 1024 | DEMON_SURGE | Dark energy column (server, Deucalion only) |

### 3.1 Klaus Mikaelson — Hybrid Body Horror

**Show reference:** Bones audibly crack and reshape; Klaus doubles over, spine arching; the hybrid form erupts from the human body mid-scream.

| Tick range | Event | New assets needed |
|------------|-------|-------------------|
| 0 | Growl + first spasm: man hunches forward, hands grip knees | `klaus_human_spasm_1` pose (Blockbench, 1 keyframe) |
| 3–5 | Spine arch: chest pushes forward, head snaps back | `klaus_human_spasm_2` pose |
| 7 | Bone crack sound + DISTORT pulse + blood mist at torso | Blood mist particle texture (8×8 grayscale) |
| 8–12 | Arms elongate visually via bone scale tween (client); DUST continues climbing | No new asset — bone transform in renderer |
| 14 | Second crack + SHAKE impulse (0.15 s, amplitude 3 px) | — |
| 16 | Final spasm: man collapses to one knee, mouth open | `klaus_human_spasm_3` pose |
| 19 | Pre-swap flash: man model emissive eyes glow white for 1 tick | Eye emissive mask addition to existing skin |
| 20 | **SWAP** — wolf appears in `shift_in` clip (existing); smoke burst (existing) | — |
| 20–25 | Wolf `shift_in` continues; DISTORT fades out over 5 ticks | — |
| 32 | Landing poof (existing) + SHAKE (0.1 s, amplitude 2 px) | — |
| 40+ | Howl (existing) | — |

**GeckoLib:** No new wolf animations. Three new man-body poses (static keyframes applied as bone overrides by the renderer, not full GeckoLib animations — keeps the man on the humanoid pipeline).

**Sound additions:** Layered `ENTITY_PLAYER_HURT` pitched down at ticks 3 and 16 under the existing `BONE_BLOCK_BREAK`.

### 3.2 Beast of Gévaudan (Sebastien Valet) — Spiritual Shift

**Show reference:** The shift is mystical, not purely physical; spirit energy wraps the body; the wolf emerges from light/shadow rather than bone-breaking.

| Tick range | Event | New assets needed |
|------------|-------|-------------------|
| 0 | Low growl + SPIRITUAL wisps begin orbiting the man | Spirit wisp particle texture (16×16, soft-edged, blue-white) |
| 4–16 | Wisps intensify; man stands rigid (no spasms — this isn't body horror); GLOW_PULSE every 4 ticks | — |
| 7, 14 | Resonant hum sound (new: `beast_spiritual_hum.ogg`, 0.3 s) instead of bone crack | Sound file |
| 16 | Wisps converge on torso; DISTORT (subtle, wave-like not jagged) | — |
| 19 | Man dissolves into wisp cloud (emissive fade-out on man model) | Man-model alpha ramp in renderer (no new texture) |
| 20 | **SWAP** — wolf fades in from wisp cloud (alpha ramp-in over 3 ticks); no smoke burst | — |
| 20–30 | Wisps linger around wolf, fading; `shift_in` plays | — |
| 32 | Gentle landing (no poof — silent settle); no shake | — |
| 40+ | Howl with spiritual reverb tail (post-process on existing howl) | — |

**GeckoLib:** No new animations. The man uses existing idle; rigidity is a bone lock in the renderer.

**Key difference from Klaus:** No blood, no bone cracks, no spasms. Pain here is *loss of self*, not physical agony.

### 3.3 Deucalion — Demon Wolf Emergence

**Show reference:** The Demon Wolf is a massive dark form that tears out of the human; Deucalion's shift is violent and dominant.

| Tick range | Event | New assets needed |
|------------|-------|-------------------|
| 0 | Deep growl + DEMON_SURGE: dark column rises from ground around man | Dark energy particle texture (16×16, black-purple, additive blend) |
| 3 | SPASM: man staggers, one hand reaches toward face | `deucalion_human_spasm_1` pose |
| 7 | Crack + DISTORT (red tint) + SHAKE (0.2 s, amplitude 4 px — strongest of all bosses) | Red-tint variant of distortion shader uniform |
| 10 | DEMON_SURGE intensifies; man silhouette visible inside dark column | — |
| 14 | Second crack + BLOOD burst at shoulders | — |
| 16 | Man pulled upward by surge (Y-offset tween +3 blocks over 4 ticks, client) | — |
| 19 | Surge peaks; man model hidden inside it | — |
| 20 | **SWAP** — demon wolf crashes down from surge apex; heavy landing poof (2× normal count); SHAKE (0.25 s, amplitude 5 px) | — |
| 20–30 | Surge dissipates downward; `shift_in` plays through fading darkness | — |
| 32 | Secondary ground impact dust ring | Ring particle layout (radial spawn in `MorphCues`) |
| 40+ | Roar (existing) | — |

**GeckoLib:** No new wolf animations. One new man-body spasm pose.

**Key difference:** Vertical movement during shift (man lifted, wolf drops). Largest camera shake. Most particles.

### 3.4 Peter Hale — Feral Rage Shift

**Show reference:** Peter's shift is fast, angry, animalistic — less ceremonial than Klaus, less mystical than the Beast.

| Tick range | Event | New assets needed |
|------------|-------|-------------------|
| 0 | Snarl + immediate SPASM: man lurches forward | `peter_human_spasm_1` pose |
| 4 | Dust burst (double count) + SHAKE (0.1 s, amplitude 2 px) | — |
| 7 | Crack + snarl layer | — |
| 8–16 | Rapid alternating spasms (ticks 8, 10, 12, 14): man twitches between two poses | `peter_human_spasm_2` pose (alternating with spasm_1) |
| 16 | Final crouch | — |
| 19 | Brief eye glow | — |
| 20 | **SWAP** — wolf snaps into `shift_in`; aggressive angle (wolf faces camera) | — |
| 32 | Hard landing + SHAKE | — |
| 40+ | Howl (existing, slightly faster playback: 0.9× duration) | — |

**GeckoLib:** No new wolf animations. Two new man-body spasm poses.

**Key difference:** Fastest-feeling shift despite same 40 ticks. Most frequent spasms. Least supernatural.

### 3.5 Non-Shifting Bosses (Mikael, Elijah, Kol, Rebekah, Finn)

These bosses do not implement `ShiftBody` and have no beast form. They have **no transformation sequence**. If future content adds shifting for any Original, the per-boss templates above provide the pattern. This doc does not invent transformations for bosses that don't have them.

---

## 4. New Art Assets Required

### 4.1 Man-Body Spasm Poses (Blockbench, humanoid rig)

| Asset | Used by | Notes |
|-------|---------|-------|
| `klaus_human_spasm_1` | Klaus | Hunched forward, hands on knees |
| `klaus_human_spasm_2` | Klaus | Spine arched back, head up |
| `klaus_human_spasm_3` | Klaus | One knee down, mouth open |
| `deucalion_human_spasm_1` | Deucalion | Stagger, hand to face |
| `peter_human_spasm_1` | Peter | Forward lurch |
| `peter_human_spasm_2` | Peter | Twitch alternate |

**Format:** Static keyframe data exported as bone rotation/scale JSON, loaded by the renderer as pose overrides during the man-half of the shift. Not full GeckoLib animations — the man stays on the humanoid rendering path.

**Effort:** ~30 min each in Blockbench. 6 poses × 30 min = **3 hours**.

### 4.2 Particle Textures

| Asset | Size | Used by | Notes |
|-------|------|---------|-------|
| `blood_mist.png` | 8×8 | Klaus | Grayscale soft circle, red-tinted in code |
| `spirit_wisp.png` | 16×16 | Beast | Soft-edged, blue-white, additive |
| `dark_energy.png` | 16×16 | Deucalion | Black-purple, additive blend |

**Effort:** ~20 min each. 3 textures × 20 min = **1 hour**.

### 4.3 Sound Effects

| Asset | Duration | Used by | Notes |
|-------|----------|---------|-------|
| `beast_spiritual_hum.ogg` | 0.3 s | Beast | Resonant tone, replaces bone crack |

**Effort:** Synthesize or source from CC0 library. **15 min**.

### 4.4 Texture Modifications

| Asset | Used by | Notes |
|-------|---------|-------|
| Klaus man skin: add emissive eye region to existing `_glowmask.png` | Klaus | 2-pixel eye highlight, white |

**Effort:** **10 min** in image editor.

### Total New Art Effort

| Category | Hours |
|----------|-------|
| Spasm poses (6) | 3.0 |
| Particle textures (3) | 1.0 |
| Sound (1) | 0.25 |
| Texture edit (1) | 0.17 |
| **Total** | **~4.5 hours** |

---

## 5. GeckoLib Animation Requirements

### What exists and is reused

| Clip | Models that have it | Reused for |
|------|---------------------|------------|
| `shift_in` | klaus_hybrid, deucalion_demon_wolf, beast_of_gevaudan, peter_hale_alpha | Wolf arrival after swap |
| `howl` / `roar` | All four | Post-shift vocalization |
| `idle`, `walk`, `run` | All four | Normal beast locomotion |
| `death` | All four | Death pose |

### What is NOT added

- **No new wolf-side animations.** The existing `shift_in` clips already show the wolf arriving; pain is conveyed on the *man* side before the swap.
- **No new GeckoLib models.** All four shifting bosses already have geo models.

### What IS added (not GeckoLib)

Man-body spasm poses are **bone rotation overrides applied by `ShiftRenderer`**, not GeckoLib clips. Rationale:
- The man uses `HumanoidBossRenderer`, not `GeoEntityRenderer`.
- Adding GeckoLib to the man side would require a second controller, a second model, and a blend system between two renderers — massive scope creep.
- Static pose overrides (set bone rotations for N ticks) achieve the visual effect with zero new infrastructure.

Implementation sketch (for the future coder, not this doc):
```
// In HumanoidBossRenderer.render(), during MorphRules.manHalf():
float progress = MorphRules.manProgress(ticks, partial);
PoseData pose = SpasmPoses.forBoss(bossId, progress); // lookup table
pose.applyTo(poseStack); // rotate/scale bones before vanilla draw
```

---

## 6. Client-Side Rendering Approach

### 6.1 Bone Transforms (Man-Half Spasms)

- `HumanoidBossRenderer` reads `MorphRules.manProgress()` and a boss-specific spasm schedule.
- Applies bone rotation/scale overrides to the humanoid model before drawing.
- Interpolates between spasm keyframes using linear lerp on the bone matrices.
- No new model, no new renderer — just a pre-draw hook.

### 6.2 Alpha Ramping (Beast Spiritual Shift)

- During ticks 16–23 for the Beast only: man model alpha fades 1→0 (ticks 16–19), wolf model alpha fades 0→1 (ticks 20–23).
- Implemented as a render-layer multiplier in `ShiftRenderer.render()`.
- Replaces the hard cut with a crossfade for the spiritual shift only; other bosses keep the hard cut (it's part of their violence).

### 6.3 Screen Distortion

- New post-process shader uniform: `sb_shift_distort` (vec2: intensity, frequency).
- Set by a client-side event handler that listens for the `DISTORT` cue bit.
- Vertex displacement shader on the boss's render pass only (not full-screen — cheaper, more focused).
- Fades out over configurable ticks after the cue fires.

### 6.4 Camera Shake

- Client-side impulse system: `ShakeImpulse(durationTicks, amplitudePixels)`.
- Triggered by the `SHAKE` cue bit via a client event.
- Applied as a view-bobbing offset in the render tick, decaying linearly.
- Amplitude and duration are per-boss (see §3 tables).

### 6.5 Emissive Pulses

- `GLOW_PULSE` cue temporarily boosts the emissive layer intensity of the boss's glowmask.
- Multiplied in the `AutoGlowingGeoLayer` shader uniform for the beast half.
- For the man half: the emissive eye addition on Klaus's skin pulses via the same bone-override system adjusting brightness.

---

## 7. Accessibility — Reduce Motion

When `Options.accessibility().reducedMotion()` is true (vanilla Minecraft setting):

| Effect | Full | Reduced |
|--------|------|---------|
| Spasm poses | Animated bone overrides | Static hunched pose held for entire man-half |
| Screen distortion | Shader displacement | Disabled entirely |
| Camera shake | Impulse system | Disabled entirely |
| Alpha crossfade (Beast) | 7-tick ramp | Instant swap at tick 20 (same as current) |
| Emissive pulses | Timed intensity spikes | Static emissive (no pulsing) |
| Particles | Full counts per cue | Halved counts |
| Sounds | All layered sounds | Only the primary cue sound per tick (no layers) |
| Timing | Unchanged (40 ticks) | Unchanged (gameplay-critical) |

**Principle:** Reduce motion removes *visual intensity*, never *information*. The player always knows a shift is happening and when it completes. The cue schedule and tick counts are identical regardless of setting.

**Implementation:** Each client-side system checks the reduce-motion flag before applying its effect. Server-side cues fire identically; only the client's response differs. This matches the existing test constraint in `MorphNoClientImportsTest`: reduce motion is interface-only.

---

## 8. Integration Points (For Future Implementation)

| Change | File | Nature |
|--------|------|--------|
| New cue bits | `MorphRules` | Add constants + expand `cues()` with boss-id parameter |
| Boss-specific cue schedules | `MorphRules` or new `BossShiftProfile` | Per-boss timing tables |
| Spasm pose loading | New `SpasmPoses` class | Loads bone rotation JSON from resources |
| Spasm application | `HumanoidBossRenderer` | Pre-draw bone override hook |
| Alpha ramp | `ShiftRenderer.render()` | Conditional crossfade for Beast |
| Distortion shader | New shader + client event handler | Post-process uniform |
| Camera shake | New `ShakeImpulse` + client tick handler | View offset |
| Sound layers | `MorphCues.play()` | Additional `playSound` calls per cue |
| Reduce motion guards | All client systems | `Options.accessibility().reducedMotion()` check |

No changes to `ShiftBody`, `ShiftCore`, `ShiftRules`, or server-side entity logic beyond expanding the cue bitmask.

---

## 9. Risk Register

| Risk | Mitigation |
|------|------------|
| Spasm poses look wrong on different player-model proportions | Test on Steve and Alex base models; poses use relative bone rotations, not absolute positions |
| Distortion shader causes FPS drops on low-end GPUs | Vertex displacement only on boss render pass; disable below configurable GPU tier; reduce-motion disables it |
| Camera shake causes motion sickness even without reduce-motion | Default amplitude is conservative (3–5 px); config option for shake intensity multiplier |
| Alpha crossfade shows z-fighting between man and wolf models | Offset wolf Y by +0.01 during crossfade window; tested in morph reference videos |
| New cue bits break existing saved shift state | Cue bits are transient (per-tick), never serialized; save format unchanged |
| Art effort estimate is optimistic | Spasm poses are the bottleneck; if they take 45 min each, total is 6 hours instead of 4.5 |

---

## 10. Open Questions (For Owner)

1. Should Peter Hale's shift be faster than 40 ticks (e.g., 30 ticks) to match his feral character, or must all bosses share timing for consistency?
2. Should the Beast's spiritual hum replace bone cracks entirely, or layer underneath them for players who want both?
3. Is the Deucalion vertical lift (man raised 3 blocks) acceptable given it may clip through low ceilings?
4. Should Klaus's blood mist leave a temporary ground decal, or is that too much for post-release scope?
5. Do we want a config option for shift intensity (subtle / normal / intense) beyond the binary reduce-motion toggle?
# Overlapping player abilities across mods (research only, nothing changed)

For the skill wheel's **rule C** (one entry, main side runs, shared cooldown, no stacking, no change to either mod's progress or costs).
Repo `SupernaturalBloodlines` main `a6e54f25`, 2026-11-22. Spec: `docs/superpowers/specs/2026-11-22-skill-wheel-design.md` (worktree `docs-wheel`).

## 0. How to read this

- **Source ids** are `mod:id`. Vampirism/Werewolves ids are the action registry ids (`vampirism:teleport`). Tensura ids are the lang/skill names (`tensura:coercion`). Infinity Stones ids are `infinity:<stone>.<node>` (the tree node key in `data/infinity/stone_trees/<stone>.json`, the same style as its wheel mode ids).
- **Kind** = the wheel's kinds: `instant`, `toggle`, `held`, `aimed`. Vampirism/Werewolves "lasting" actions (`ILastingAction`) are shown as `toggle (timed)`: they switch on, run for a configured duration, and the **cooldown starts when they end**. That matters for rule C (see section 6).
- **Confidence:** `same` = the same player-facing effect; `close` = same purpose, different numbers or mechanics; `loose` = shares a theme only. Only `same` and `close` are in section 1. `loose` is in section 3 and is **not** proposed for the dedup.
- **Proposed main** follows the spec default: Vampirism for a Vampirism vampire, Werewolves for a Werewolves werewolf, Tensura when it is their race skill. Where two defaults collide I say so and mark **owner call**.
- Moot-pair note: Vampirism's API holds one faction per player, and our hybrid sits in the Werewolves faction. So a Vampirism-vs-Werewolves pair (rage, for example) can only meet in a player who somehow has both. The pairs that really meet are faction or Tensura race vs Tensura skills, addons and Infinity Stones.

### What was scanned

| Source | Version | What I read | Result |
|---|---|---|---|
| Vampirism | 1.10.13 | `VampireActions`, `HunterActions`, `LordActions` and each action's class (sources jar); `VampireSkills`, `HunterSkills`; lang; vision registry | 14 vampire + 3 hunter + 2 lord actions; 2 non-action toggles (vision key, suck key) |
| Werewolves | 2.0.3.4 | `ModActions` + each action class, `ModSkills`, lang | 9 actions (3 forms, howling, rage, sense, fear, leap, hide_name); night vision is a skill toggle, bite is a key |
| Tensura | 2.0.1.5 | `javap` hook scan of 217 classes under `ability/skill`; lang names and descriptions; `javap -c` of the 5 races that override `onActivateAbility` | 169 skills mapped to lang ids, **145 active or toggle** (common 14, extra 52, intrinsic 23, unique 56). **No ultimates in base.** Resistances skipped. Invisibility is a *spell* (`InvisibleMagic`), not a skill |
| Tensura race abilities | base | Lesser Daemon, Harpy, Dragonewt, Slime, Vampire (+ Overcomer, Lord, Divine inherit `VampireRace`, none overrides) | see rows M03, M04 and section 3 |
| Ascension 3.1.0 | | `ascension_skills.txt`, `skilltypes.txt` (docs/plan-19) | 61 hook classes; read all descriptions |
| Great Sage 0.0.7.1 | | lang + class list | Raphael (ultimate), Venuzdonoa (sword item/skill), keys: assess, scan, voice, menu, panic |
| Horizons 0.31 | | skills + races + hook scan | 21 hook classes; Phoenix and Wyvern race abilities |
| Mysticism 2.2.1 | | skills, races, hook scan | 96 hook classes (helpers included); 26 race classes with a race ability, mostly flight; only keyword-matched descriptions read in full |
| KumoDesu 2.0.3-HF2 | | all 14 skills | only `spider_movement` (passive) is relevant |
| SlimeThrone Extras 2.1.3/2.1.4 | | skills + races + hook scan | `keen_nose`, Mystic Fox speed toggle, Wisp mode |
| FoxAblaze 1.0.6.8 | | 15 ultimates, description text | only Yog-Sothoth teleport is relevant |
| Rebalanced 3.2.17 | | jar listing | **adds no abilities** (a config patcher: `META-INF/rebalance/active|inactive`) |
| Infinity Stones 8.0.0-alpha.6 | | source: `stone_trees/*.json`, lang, `AbilityMeta` | read the nodes that match the categories (about 25 of ~200 powers) |
| Ours | main + `plan-18e` worktree | `ExtraActions`, `parts/hud`, `CoveredKeyIds`, `BiteRules`, skill data (46 skills), lang | see section 4 |

Work files (not needed to read this): `tensura_skills.csv`, `tensura_skill_hooks.txt`, `addon_hooks.txt`, `inventory_notes.md` in this folder.

## 1. Main table: proposed deduplication (same or close)

| # | Power | Sources (id, kind): effect | Conf | Proposed main and why |
|---|---|---|---|---|
| M01 | **Invisibility** | `vampirism:vampire_invisibility` toggle (timed): `setInvisible(true)` for the configured duration, cooldown after.<br>`infinity:reality.invisibility` toggle: "truly invisible", no cooldown (Palladium `invisibility`).<br>`tensura:invisible` (**magic spell**, aspectual/illusion, `InvisibleMagic`): "turn the caster invisible". | Vampirism vs Infinity: **same**.<br>Tensura spell: **close** | **Vampirism** for a Vampirism vampire (their main kind's action); **Infinity** otherwise. The Tensura spell stays on Tensura's magic wheel: spells are not in scope, **owner call** if they should join. |
| M02 | **Night vision** | `vampirism:night_vision` toggle (vision key V, `IVampireVision`): night vision for a vampire. **Not on the wheel today** (spec leaves `keys.vampirism.vision` alone).<br>`infinity:mind.night_vision` toggle (persistent): gives `minecraft:night_vision` infinite while on.<br>`tensura:godwolf_sense` toggle (extra): see in the dark **and** spot invisible entities nearby. Our **S-WOL-3 grants this** to werewolves.<br>`werewolves:night_vision` is a passive skill flag (form only, no key): see passive list. | Vampirism vs Infinity: **same** (same effect).<br>Godwolf Sense: **close** (adds invisible-entity sight) | **Vampirism** for vampires; **Tensura** `godwolf_sense` for werewolves (their own tree grants it, and Werewolves has no toggle to show). Only matters once Vampirism's vision is on the wheel. Infinity last. |
| M03 | **Bat form** | `vampirism:bat` toggle (timed): bat form and flight, refused in sun, water, riding; gates `summon_bat`.<br>Tensura Vampire race ability (`VampireRace.onActivateAbility`, key `tensura.keybinding.race_ability`, effect `tensura:bats_mode`): toggles Bats Mode with flight (`RaceUtils.canStillFly`). All four vampire races inherit it. | **close** (both turn the vampire into a flying bat; the Tensura side's model and limits were not run in game) | Collision: a player who is both a Vampirism vampire and a Tensura vampire race. Proposed main **Vampirism** (action, and its sun and water rules are the ones our docs know); **owner call**. The Tensura race-ability key is left alone by the spec, so the race ability would have to be added to the wheel for this to apply. |
| M04 | **Flight (not bat)** | `tensura:gravity_flight` held (common): gravity flight keeping momentum.<br>`tensura:gravity_manipulation` / `gravity_domination` toggle (extra): "fly without hindrance".<br>`ascension:angel_wings` toggle (extra): glide like an elytra, without one.<br>`infinity:mind.flight_toggle` + `mind.flight-dummy` toggle, speed level 1-10 (Mind Stone).<br>Tensura race flight: Lesser Daemon (toggle), Harpy and Dragonewt (boost); `tensurahorizons` Phoenix and Wyvern (boost, glide); Mysticism angels, wasps, drone beetles, wyrms, dragonoid (race ability). | **close**. Mechanics differ (held vs toggle vs glide vs boost). Same-mod pairs (gravity flight vs gravity manipulation) are not a cross-mod dedupe | **Tensura** (their race skill or skill). Infinity's flight is the stone's own mode and should hide behind Tensura's only for a player who has both, **owner call**. Do **not** merge boost-style race abilities into toggles. |
| M05 | **Regeneration** | `vampirism:regen` toggle (timed): Regeneration I (II refined) pulses every 2.5 s, uses more blood.<br>`tensura:self_regeneration` toggle (common), `ultraspeed_regeneration` and `infinite_regeneration` toggle (extra): faster to near-instant natural regen, cost magicule.<br>`horizons:rapid_regeneration` toggle (2% max HP/s, 300 magicule) and `horizons:reconstruction` toggle (heals after 3 s without damage).<br>`infinity:soul.regen`: gives `regeneration` level 20, infinite, and `instant_health`, while on. | **close** (a heal-over-time toggle). Vampirism vs Infinity: **same** (the Regeneration effect) | **Vampirism** for vampires; **Tensura** `self_regeneration` for Tensura players; addons and Infinity last. Mind the Infinity `effect clear @s` hazard, section 6. |
| M06 | **Rage** (speed + strength buff) | `vampirism:vampire_rage` toggle (timed): Speed III, Strength I, Haste I each second, more blood use, refused in bat form.<br>`werewolves:rage` toggle (timed): Speed I (+1 survivalist), Strength I (+1 beast), plus extra bite damage.<br>`horizons:core_overdrive` instant-timed (10 s): +15% move, +25% attack speed, +15% damage.<br>`ascension:blood_frenzy` toggle: hit harder and faster while HP bleeds, kills heal.<br>`tensura:ogre_berserker` (intrinsic, press): rage then aftereffects; `royal_beast`, `divine_berserker` (unique, press).<br>`mysticism:lightning_mode` (intrinsic): burst speed, damage, drawbacks. | Vampirism vs Werewolves: **same**.<br>The rest: **close** | The **faction's own rage** (Vampirism for vampires, Werewolves for werewolves); Tensura and addon rages only when the player has no faction rage. Aftereffect skills (`ogre_berserker`, `divine_berserker`) are close at best: keep them separate if the owner wants their drawbacks visible. |
| M07 | **Frighten nearby enemies** | `werewolves:fear` instant: mobs in 10x3x10 are frightened and run away.<br>`ascension:intimidating_roar` instant: terrify and weaken nearby enemies.<br>`tensura:coercion` instant (common): blast of roar scaring afflicted entities.<br>`infinity:mind.terror_projection` instant (cd 8 s): cone, up to 8 hostile minds flee for 5 s, 60 damage.<br>`tensura:mortal_fear`, `hero_haki`, `demon_lord_haki`, `sacred_haki` held (extra): haki auras that scare weaker enemies. | Werewolves vs Ascension: **same**.<br>Coercion and terror projection: **close**.<br>Haki: **close** (fear is one mode of a larger skill) | **Werewolves** `fear` for werewolves; **Tensura** `coercion` otherwise. Keep the haki skills separate (held auras with allies and other modes). |
| M08 | **Teleport to where you look** | `vampirism:teleport` aimed: teleports to the looked-at spot (config range, refinement extends), refused in bat form.<br>`horizons:void_step` aimed: up to 25 blocks, not through walls, cd 8 s (6 mastered).<br>`infinity:space.teleport` aimed (press): teleport to wherever you look.<br>`foxablazeultimate:yog_sothoth` aimed (press): teleports behind the targeted creature (up to 40 blocks, 5 s cd).<br>`tensura:traveler` (unique) and `tensura:suppressor` (unique, blink mode): teleport is one **mode** of a bigger skill. | Vampirism vs Horizons vs Infinity: **same**.<br>Yog-Sothoth: **close** (behind the target).<br>Traveler and Suppressor: **close** | **Vampirism** for vampires; otherwise the **Tensura-side** entry (race skill) and addons. Tensura unique modes are one wheel entry of the skill, not a separate power, so the rule C row would attach to a mode: **owner call** how modes show. |
| M09 | **Blood projectile** | `vampirism:dark_blood_projectile` instant: fires a damaging blood projectile (refinements: AoE ring, multi-shot, pierce).<br>`ascension:blood_blade` instant (extra): fires a crimson blade, costs HP instead of magicule. | **close** | **Vampirism** for vampires. Ascension's costs HP, so merging it would hide a cost: keep both unless the owner accepts that. |
| M10 | **Sense nearby creatures through walls** | `werewolves:sense` toggle (timed): sense nearby mobs through walls.<br>`tensura:magic_sense` toggle (extra): sense entities around with magicule (our **S-WIT-5 grants it**).<br>`tensura:sense_heat_source` toggle (extra): highlights entities that generate heat; `universal_perception` (extra toggle) combines magic, sound and heat sense. | Werewolves vs Magic Sense: **close**.<br>Heat source: **close** | **Werewolves** `sense` for werewolves; **Tensura** otherwise. `horizons:pit_sense` (visible only, no walls) is loose, section 3. |
| M11 | **Appraise a target** | `greatsage:assess` / `scan` (keys, not skills; the mode is labelled "ANALYTICAL APPRAISAL - ACTIVE").<br>`tensura:analytical_appraisal` instant (extra): assess opponents' strength.<br>`tensura:great_sage` unique (modes include appraisal).<br>`ascension:judgement_bird` toggle (Scales of Sin: analyze what you look at). | Great Sage vs Analytical Appraisal: **close**.<br>Judgement Bird: **close** | **Tensura** `analytical_appraisal` (the mod Great Sage builds on). Great Sage's own keys are covered, so nothing is lost. |
| M12 | **Bite / drain blood** | **Ours** `bite` (see section 2), held-aimed: Vampirism suck, Werewolves bite, hybrid feed and the turning bite.<br>Tensura Vampire race ability (`VampireRace.onActivateAbility`): targeted (5 blocks) blood drain on a non-bloodless creature (read from bytecode: target pick, `isBloodless`, health and food checks; not run in game). Already **credits Vampirism blood** (`TensuraFeed`, switch `tensura_feed_fills_blood`). | **close** (same act, but the Tensura one is on the race-ability key, and its source of blood is the target's Vampirism blood or food) | **Ours (Bite)**: it already is the one bite entry and Tensura's drain is already merged into Vampirism's blood by `TensuraFeed`. Not applicable until the race ability is on the wheel. |

Counts: **12 groups** in the main table. **6 have a `same` pair** (M01, M02, M05 via the Infinity pair, M06, M07, M08); **6 are `close` only** (M03, M04, M09, M10, M11, M12).

## 2. Second list: abilities we ALREADY replace (with evidence)

| # | Our entry replaces | Evidence | What it does and does not do |
|---|---|---|---|
| R1 | **Bite** replaces Vampirism **suck** (`keys.vampirism.suck`) and Werewolves **bite** (`keys.werewolves.bite`) | `core/hud/CoveredKeyIds.java`: both keys are covered. `core/hud/BiteRules.java` (R110): by form, first match wins: werewolf in wolf form -> `WEREWOLF_BITE`; Vampirism vampire -> `VAMPIRISM_DRINK`; hybrid type -> `HYBRID_FEED`; turning -> `TURNING_BITE`; werewolf faction -> `WEREWOLF_BITE`; else `NONE`. `parts/hud/bite/VampirismBite.java` starts Vampirism's own feeding by method handle (`biteEntity`, `endFeeding`); `WerewolfBite.java` calls Werewolves' `canBite`, `canBiteEntity`, `bite`. | We replace the **key and the entry**. The effect is still each mod's own bite code (they run, we choose which). The Tensura race drain is **not** replaced: only merged for blood (`TensuraFeed`). |
| R2 | **Sprint-leap** replaces the Werewolves **leap key** (`keys.werewolves.leap`) | `CoveredKeyIds.all(sprintLeap)` covers it while the client setting `sprint_leap` is on. `core/hud/SprintLeap.java` + `parts/hud/client/werewolves/SprintLeapClient.java`: jump while sprinting sends the **same packet** Werewolves' key sends and toggles the same `werewolves:leap` action. `docs/parts/hud.md` "Werewolves' Leap and the sprint leap (PR #50)". | It replaces the **input**, not the power. The leap is still `werewolves:leap` (lasting; +0.05 flying speed and jump boost; no target, no damage). Hybrid Leap (below) is a **different** power, separate by design (plan 18e: "stay separate and share one guard"). |
| R3 | **Alpha Roar** slot replaces our own **Alpha Roar key** | `CoveredKeyIds.ALPHA_ROAR_KEY`; `shared/hud/ExtraActions.ALPHA_ROAR`; `core/hud/Loadout` puts it in the first empty slot once; `client/PackKeybinds.java` keeps the key for a game without the HUD. | Not another mod's power; included because the spec names it. |
| R4 | The 9 **Vampirism action keys** (`keys.vampirism.action1..9`) | `CoveredKeyIds.all`; `parts/hud/action/FactionActions.java` lists and toggles unlocked Vampirism and Werewolves actions through `IActionHandler`. | The key is replaced; the actions run through Vampirism's own `toggleAction`. Vampirism's R wheel key (`keys.vampirism.action`) is **not** covered in code today (the spec says it will be). |
| R5 | **Tensura skill keys** 1-3 and the `tensuraslots` secondary keys | `CoveredKeyIds.all` (`tensura.keybinding.ability.slot_1..3`, `tensuraslots.keybinding.secondary_ability.slot_1..3`); `parts/hud/client/tensura/*`. | Input only. |
| R6 | **Tensura race-ability blood drain** is **merged**, not replaced | `parts/hud/bite/TensuraFeed.java` (plan 13c): the Tensura vampire drain fills Vampirism blood through `drinkBlood`. | One effect, two credits: the feed runs on Tensura's key; we only bridge the blood. |
| R7 | **Our granted Tensura skills are the same id as Tensura's**, so they cannot show twice | S-VAM-5 -> `tensura:blood_mist`, S-WIT-5 -> `tensura:magic_sense`, S-HUN-4 -> `tensura:danger_sense`, S-WOL-3 -> `tensura:godwolf_sense`, S-VAM-7 -> `vampirism:less_sundamage`. | One skill, one entry. The risks are against the *other* mod's alternative (rows M02, M10 and L1). |

Counts: **7 rows** (R1-R2 are the two the spec names; R3-R7 are the same pattern or merges I found).

## 3. Loose matches: NOT proposed for dedup (kept separate unless the owner says otherwise)

| # | Theme | Pair or group | Why only loose |
|---|---|---|---|
| L1 | Threat warning | `vampirism:awareness_hunter` (toggle: detects nearby **vampires**) vs `tensura:danger_sense` (**our S-HUN-4 gives it to hunters**) vs `werewolves:sixth_sense` (passive screen flash) | Different target set (vampires vs any hostile). Worth an owner look: it is the one loose pair the **same player is given by our own tree**. |
| L2 | Pack roar buff | `werewolves:howling` (attack speed for you and nearby werewolves, plus wolves with `wolf_pack`) vs **ours** `alpha_roar` (Betas get Strength and Speed; other werewolves forced out of form) vs `moonblood_surge` (pack buff within 24 blocks, pays afterwards) vs `vampirism:lord_attack_speed` / `lord_speed` (faction allies within 10 blocks) vs `tensura:hero_haki` (inspire allies) | Different buffs. Alpha Roar is built on the pack system, not on Howling. |
| L3 | Summon wolves | `werewolves:howling` + `wolf_pack` skill vs **ours** Wolf Spirits (an Alpha calls three spirit wolves for three minutes: `/pack spirits`) | Howling is a buff **and** a summon; only half overlaps. Wolf Spirits is not a slot action today. |
| L4 | Leaps | `werewolves:leap`, `vampirism:jump_boost`, Tensura Slime race ability (charged jump), `infinity:power.gravity_hammer` (leap then crash), `horizons:solar_dive` (dash up to 12 blocks) vs **ours** Hybrid Leap (18e: aimed, range 12, 6 damage, Weakness 5 s, cooldown 20 s) | Hybrid Leap is a targeted attack; the others are movement. **Plan 18e already decided to keep them separate.** |
| L5 | Disguise | `vampirism:disguise_vampire`, `vampirism:disguise_hunter`, `werewolves:hide_name` (hides the name in form), `tensura:falsifier` (conceal presence), `tensura_kumodesu:humanification` | Different things hidden (face vs name vs presence). |
| L6 | Damage cap | `vampirism:half_invulnerable` (blocks large hits, slows you) vs `ascension:god_of_fortune` (no hit above a fifth of HP) vs `horizons:lifeflame_mantle` (-20% damage, heal) | Different mechanics. |
| L7 | Concealment while moving | `tensura:shadow_motion`, `tensura:murderer`, `tensura:shadow_striker`, `infinity:mind.phantasm`, `infinity:mind.psychic_avatar` vs M01 invisibility | Invisibility is part of the effect, not all of it. |
| L8 | Kill the weak | **ours** Rip Heart (kills a creature at or under N hearts; Originals and bosses immune) vs `tensura:merciless` (instantly kill weakened enemies) vs `foxablazeultimate:beelzebuth` (consume weak) | Merciless is one mode of a skill. |
| L9 | Bite mode inside bigger skills | `ascension:deus_sanguis` (crippling bite mode, bat-shrink mode), `tensura:drain` (steal vitality and a skill), `tensura:blood_mist` | Modes of larger skills. |
| L10 | Scare/push | `vampirism:hissing` (mobs targeting you let go) vs `horizons:draconic_roar` (push and slow 30%) vs M07 | Disorient or push, not fear. |
| L11 | Sense by sight | `horizons:pit_sense` (reveal visible enemies, no walls), `ascension:big_bird` (outline creatures within 100 blocks, even invisible), `stextras:keen_nose` (valuables) vs M10 | Different ranges and targets. |
| L12 | Forms | `werewolves:beast_form` / `survival_form` / `human_form` vs `tensura:beast_transformation` (intrinsic) | A form pack vs a one-off transform. |
| L13 | Speed toggle | `stextras:mystic_fox` race ability (a Speed potion effect toggle) vs M06 | Pure speed, no strength. |

Counts: **13 loose rows**.

## 4. Our abilities with no overlap found

- **Neck Snap** (aimed; a vampire-type lies as dead for a while, a mortal at or under N hearts dies, a healthier one staggers): none.
- **Bottle my blood** (instant): none (`tensura:blood_mist` spills your blood but as an attack: L9).
- **Demon Wolf Roar** (`S-WOL-11`, rides the Alpha Roar, forces other player Alphas to shift): none.
- **Sire's Call** (`S-ORI-3`, `/bloodline call`): none.
- **Hybrid Leap**: loose only (L4). **Moonblood Surge** and **Wolf Spirits** are hub and command powers today (`/pack surge`, `/pack spirits`), not `ExtraActions`, so they are not on the wheel yet.
- Slot action ids in main: `alpha_roar`, `rip_heart`, `neck_snap`, `bottle_blood` (`ExtraActions`); `bite` (HUD); `hybrid_leap` arrives with 18e.

## 5. Passive overlaps (not on the wheel)

| Effect | Sources |
|---|---|
| Movement speed | `vampirism:vampire_speed`, `werewolves:speed`, `infinity:space.speed` (+0.05 attribute), `infinity:time.chronal_mastery` (+80%), ours `S-HUM-3` Swift Feet (+5%), `S-VAM-2` Night Stride (+5% at night) |
| Max health | ours `S-HUM-1`, `S-VAM-4`, `S-WIT-7` (+2 each); `infinity:soul.health` (nearly infinite); `infinity:space.spatial_sovereignty` (+60) |
| Attack damage | ours `S-ORI-1` Old Blood (+10%), `S-WOL-7` Moon Heart (+10% full moon); `vampirism:vampire_attack_damage`, `hunter_attack_damage`; `werewolves:damage` |
| Attack speed | `vampirism:vampire_attack_speed`, `hunter_attack_speed`, `hunter_attack_speed_advanced` |
| Armor | ours `S-WOL-1` Thick Hide (+1); `werewolves:thick_fur`, `resistance` |
| Sun and holy | `vampirism:less_sundamage` (our `S-VAM-7` grants it), `sunscreen` action effect; ours `S-AUG-2` Blessed Skin |
| Night vision | `werewolves:night_vision` (form), `tensura_kumodesu:spider_movement` (also climbing, cobweb immunity) |
| Step up / climb | `infinity:space.step_assist`, `werewolves:climber` (toggle skill), `kumodesu:spider_movement` |
| Jump | `vampirism:vampire_jump`, `werewolves:jump` |
| Self-heal | `werewolves:health_reg` (and health after kill), `horizons:renewing_flames` (passive) |
| Danger | `werewolves:sixth_sense`, `tensura:danger_sense` (also L1) |

## 6. Risks for rule C that the table does not show

1. **Timed cooldown.** Vampirism and Werewolves "lasting" actions start their cooldown when they **end**. A Tensura or Infinity toggle has none. "Both sides go on cooldown" for a toggle partner means a new cooldown has to be invented for it (or the shared cooldown starts at turn-off for all sides).
2. **Infinity hazard.** `infinity:soul.regen` runs `effect clear @s` when it turns off. That clears **every** effect (Vampirism rage, Werewolves rage, ours). Rule C's "never stack" will not protect from that.
3. **Infinity persistent effects.** `mind.night_vision`, `soul.regen`, `space.speed`, `mind.flight` are tagged `passive` in the tree JSON but act as persistent toggles. The wheel treats them as toggles; "no stacking from any key" has to include the stone key, not only the wheel.
4. **Keys left alone.** The spec leaves the Vampirism vision key and the Tensura race-ability key alone. Rows M02, M03 and M12 only apply if those two go onto the wheel. Otherwise their Tensura or Vampirism side stays off the wheel and nothing dedupes.
5. **Modes.** Tensura unique skills (`traveler`, `suppressor`, `deus_sanguis`) carry several modes. Rule C pairs whole actions: a mode inside a skill needs the wheel to show modes as entries first.
6. **Same mod, many versions.** Tensura has several tiers of one power (`self_regeneration` -> `ultraspeed` -> `infinite`). Those are Tensura's own upgrade chain, not an overlap; the data file should list the highest the player has.
7. **Vampirism cross-faction.** M06 and similar rows list Vampirism and Werewolves together for completeness. They cannot meet in one player unless a player can be both.

## 7. What I did not check

- Vampirism and Werewolves config **numbers** (durations, cooldowns): not read, only the kind and effect.
- Whether the Tensura **Bats Mode** model matches Vampirism's bat in game.
- Mysticism: 96 hook classes (includes helper classes). I read the descriptions of the keyword-matched and a sample of the others; the rest are named only (Pokemon-themed uniques and the like).
- Ascension skills that are **modes** of uniques (for example Deus Sanguis) were taken from the descriptions, not run.
- Infinity Stones: only the nodes that matched the categories (the `extras` and `port` folders were not diffed one by one).
- Tensura magic spells (about 130 in `ability/magic`) are out of the brief; only invisibility is mentioned.
- No game was started: everything is from jars, source and lang files.

# Balance

Every number the supernatural system pays or asks for, with the reason for it. The numbers started as the designer's drafts (`docs/superpowers/specs/2026-11-14-supernatural-system-design.md`); on 2026-11-16 the owner asked for them to be finished, and this file is the result of that pass. `BalanceInvariantsTest` checks the rules below on the shipped data, so a new rung, skill or task that breaks one fails with its id. The rows of the spec and `LadderSpecRowsTest` / `SkillDataTest` pin the exact values.

## The rules the numbers follow

| Rule | Value | Why |
|---|---|---|
| An apex rung pays | 5 SP / 100,000 EP, in every ladder | The apex race itself costs 2,000,000 EP (owner). The rung is a nudge (5%), not a replacement for Tensura's own progression. |
| A whole ladder pays at most | 2,000,000 / 7 EP (about 14%) | The biggest (werewolf, 266,000) is 13%; a witch-werewolf climbing two ladders gets about 22%. |
| SP and EP per rung | never fall as the rungs climb | A later rung is harder; paying less for it would read as a bug. |
| Longest wait | 60 game days (20 hours of play at 20 minutes a day) | Reachable in a week or two on a normal server; every apex rung asks 60 or less. |
| Skill cost | 1 to 5 SP; only a granted skill (S-WOL-8) is free | Nothing is free, nothing costs a whole ladder rung. |
| Skill cost by rung | a rung-1 skill costs at most 2, a rung-5 skill at least 4 | Costs rise with the rungs. |
| A tree costs | 80% to 110% of what its ladder pays | A player who climbs the whole ladder can buy nearly the whole tree and carries little spare. |
| EP boost | x1.25 (config `ep_boost`), True Alpha x1.5 on `alpha_primeval` | Owner rules, unchanged. |

## Ladders

| Ladder | Rungs | SP | EP | Longest task (days) |
|---|---|---|---|---|
| ghoul | 1 | 0 | 0 | - |
| heretic | 6 | 20 | 210,000 | 60 |
| human | 3 | 5 | 4,000 | - |
| hunter | 6 | 18 | 171,000 | - |
| hybrid | 6 | 18 | 175,000 | 7 |
| original_hybrid | 6 | 21 | 220,000 | 60 |
| original | 6 | 20 | 210,000 | 60 |
| siphoner | 6 | 19 | 175,000 | 60 |
| tribrid | 6 | 20 | 230,000 | 60 |
| vampire | 7 | 23 | 212,000 | 60 |
| werewolf | 7 | 22 | 266,000 | 60 |
| witch | 6 | 19 | 175,000 | - |

The per-rung rows (SP, EP, tasks and their targets) are the spec's ladder tables.

## Skills

| Id | Name | SP | Value | Why |
|---|---|---|---|---|
| S-HUM-1 | Survivor | 1 | attr | a human has 5 SP in all; +2 health is the first reason to climb |
| S-HUM-2 | Vervain Blood | 2 | duration_ticks=24000 | doubles a consumable, as cheap as a tool upgrade |
| S-HUM-3 | Swift Feet | 2 | attr | small, shared with the hunter |
| S-HUN-1 | Steady Aim | 2 | prey_damage_factor=1.05 | priced by its rung and kind |
| S-HUN-2 | Hunter's Eye | 2 | sense_radius=32 | priced by its rung and kind |
| S-HUN-3 | Patient Hunter | 3 | instinct_days=5 | priced by its rung and kind |
| S-HUN-4 | Danger Sense | 3 | tensura | priced by its rung and kind |
| S-HUN-5 | Mark Mastery | 4 | mark_attack=0.25, mark_other=0.125 | priced by its rung and kind |
| S-HYB-1 | Steady Thirst | 2 | thirst_rate_factor=0.5 | priced by its rung and kind |
| S-HYB-2 | Venom Fang | 3 | slowness_ticks=100 | priced by its rung and kind |
| S-ORI-1 | Old Blood | 3 | attr | a permanent +10% attack: dearer than before (2 to 3) and one rung later (1 to 2), because Moon Heart's +10% on full-moon nights costs 4 |
| S-ORI-2 | Bloodmaker's Yield | 1 | bottles=2 | a resource for rites, not for fights |
| S-ORI-3 | Sire's Call | 3 | cooldown_ticks=6000 | priced by its rung and kind |
| S-ORI-4 | Primordial Mind | 4 | magicule_bonus=0.05 | +0.05 magicule is small: costs what every rung-5 skill costs (5 to 4) |
| S-SIP-1 | Deep Draw | 2 | cap=1500 | priced by its rung and kind |
| S-SIP-2 | Quick Draw | 2 | rate_factor=1.5 | priced by its rung and kind |
| S-SIP-3 | Life Siphon | 3 | power_per_health=100 | priced by its rung and kind |
| S-SIP-4 | Spent Wisely | 3 | payment_factor=0.9 | priced by its rung and kind |
| S-SIP-5 | Void Hunger | 4 | weakness_ticks=100 | priced by its rung and kind |
| S-VAM-1 | Keen Fangs | 1 | extra_blood=1 | the cheapest skill: +1 blood a feed, nothing a vampire cannot already do |
| S-VAM-2 | Night Stride | 2 | attr | priced by its rung and kind |
| S-VAM-3 | Silver Tongue | 2 | discount_gossip=30, duration_factor=1.5 | a social perk: capped by vanilla gossip anyway |
| S-VAM-4 | Hardened | 3 | attr | priced by its rung and kind |
| S-VAM-5 | Blood Mist | 3 | tensura | priced by its rung and kind |
| S-VAM-6 | Predator's Thirst | 4 | blood | needs its quest as well: the strongest feeding rule, so the dearest rung-4 skill |
| S-VAM-7 | Elder's Skin | 4 | vampirism | grants Vampirism's own `less_sundamage` |
| S-WIT-1 | Steady Circle | 2 | cost_factor=0.95 | priced by its rung and kind |
| S-WIT-2 | Wide Circle | 2 | altar_range_factor=1.75 | priced by its rung and kind |
| S-WIT-3 | Ashwarden | 2 | rule | priced by its rung and kind |
| S-WIT-4 | Emissary's Bond | 3 | discount=0.2 | priced by its rung and kind |
| S-WIT-5 | Magic Sense | 3 | tensura | priced by its rung and kind |
| S-WIT-6 | Ancestral Voice | 4 | cost_factor=0.5 | priced by its rung and kind |
| S-WIT-7 | Coven's Ward | 4 | aura_bonus=0.05 | priced by its rung and kind |
| S-WOL-1 | Thick Hide | 1 | attr | priced by its rung and kind |
| S-WOL-2 | Moon Calm | 2 | shift_chance_factor=0.75 | priced by its rung and kind |
| S-WOL-3 | Godwolf Sense | 2 | tensura | priced by its rung and kind |
| S-WOL-4 | Wolfsbane Tolerance | 2 | duration_factor=0.5 | priced by its rung and kind |
| S-WOL-5 | Pack Heart | 4 | betas_needed=2 | a stronger pack bonus: 3 to 4, like the other rung-4 pack skills |
| S-WOL-6 | Roar's Echo | 3 | cooldown_ticks=800 | priced by its rung and kind |
| S-WOL-7 | Moon Heart | 4 | attr | the dearest werewolf attack bonus: only on full moon nights |
| S-WOL-8 | Primeval Roar | 0 | pack_attack=0.1, pack_ticks=600, range_factor=2, cooldown_ticks=800 | granted, never bought (True Alpha on `alpha_primeval`) |

## Apex races

| Id | Health | Attack | Speed | Skill | Why |
|---|---|---|---|---|---|
| primordial_vampire | 1000 | 20 | 0.02 | infinite_regeneration | plan 05 |
| primordial_hybrid | 1000 | 25 | 0.03 | ultraspeed_regeneration | plan 05 |
| moonblood_hybrid | 900 | 25 | 0.03 | godwolf_sense | plan 05 |
| ancestral_witch | 800 | 10 | 0.01 | magic_sense | a magic race: the lowest health |
| void_siphon | 900 | 15 | 0.02 | gluttony | plan 05 |
| tribrid_ascendant | 1100 | 25 | 0.03 | ultra_instinct | three bloods: the highest |
| eternal_hunter | 900 | 20 | 0.02 | danger_sense | plan 05 |
| elder_vampire | 900 | 20 | 0.02 | self_regeneration | a plain vampire, one step under the Primordial Vampire |
| alpha_primeval | 900 | 25 | 0.03 | godwolf_sense | a plain werewolf must not out-stat the hybrid apex before the True Alpha x1.5 (1000 to 900) |

## Stacked sparks and the absent leader (plan 14)

| Value | Setting | Why |
|---|---|---|
| Sparks one wolf can hold | `max_stacked_sparks` = 5 | Four steals on top of the own spark. Each is a kill of an Alpha from outside the pack, a rare and risky act: five is a long hunt, and a cap keeps one wolf from out-scaling everything. 1 turns it off. |
| Bonus per spark past the first | +10% of the Alpha attack (+20%) and health (+4) bonuses, so +2% attack and +0.4 health each (five sparks: +28% attack, +5.6 health, with a True Alpha ×1.1 on top) | Small on purpose: the first spark is the rank, the stacked ones are a trophy, not a second rank. The numbers stay below a True Alpha''s. |
| Roar tier per spark past the first | +1 Strength tier for the pack (Strength V at five) | The pack feels the leader''s stack, not only the leader. The Roar still lasts 30 s with a 60 s cooldown. |
| True Alpha | own spark never taken, stacked ones can be | Keeps the rule that killing a True Alpha is just a death, and the True Alpha''s reward is not a thing to farm. |
| Absent leader | `absent_leader_days` = 7 (0 = off) | A week is a normal break for a player and a long enough one for a pack to need a leader. The old leader keeps the spark and can challenge for the lead, so it costs them a seat, not the rank. |

## What the 2026-11-16 pass changed

| Id | Before | After | Why |
|---|---|---|---|
| S-ORI-1 Old Blood | 2 SP, rung 1 | 3 SP, rung 2 | A permanent +10% attack was cheaper than a full-moon-only one. |
| S-ORI-4 Primordial Mind | 5 SP | 4 SP | Every other rung-5 skill costs 4, and +0.05 magicule is the smallest of them. |
| S-WOL-5 Pack Heart | 3 SP | 4 SP | The werewolf tree cost 74% of its ladder, the lowest; the pack bonus is a rung-4 effect. |
| L-WOL-5 Alpha | 6 SP | 5 SP | SP fell from 6 to 5 on the next rung; the apex rung pays 5, so the Alpha does too. |
| L-WOL-6b days as a werewolf | 100 | 60 | Every other apex rung asks 60; 100 days is 33 hours of play. |
| `alpha_primeval` health | 1000 | 900 | A plain werewolf out-stat the hybrid apex (900) before the True Alpha bonus. |

Kept as they were, because the owner set them: the apex EP (2,000,000), the EP boost x1.25, the True Alpha x1.5, the True Alpha conditions (max level, 30 days as a Beta, 300 defender kills), S-WOL-8 being granted and not bought, and the rite and quest rules of the spec. Checked and left alone: every other draft value (the vampire feed blood 3, the Rite of Revelation's 1,000 power, the hub key J, the siphon and rite numbers).

## Original lines (PR #34)

Builder numbers, all tunable: Rite of the Broken Line 12000 power (2x Severance's 6000); Rite of the Imprisoned Original 36000 power (6x), 30 s hold (`imprisonment_rite_seconds`), ring radius 8 (`imprisonment_ring_max_radius`); NPC line weights equal (`npc_line_weights`). The Imprisoned rite is harder because it frees a branch and needs the Original held.

## Plan 15a: the moon calendar (R140 to R142)
- `delayed_awakening_nights` 1 (range 0 to 3): the gene's kill is a deliberate act, so waiting one dusk keeps it from being a mid-fight surprise; 0 restores the old instant awakening.
- `full_moon_sleep` `off`: the owner decided on werewolves only as the scope when it is turned on; it is off by default so no world changes silently. Dusk is 12000, before the 12786 forced-shift window, so nobody sleeps through the night's start.

## Plan 15b: NPC packs, the weaker spark, Surge and Spirits (R143 to R146)
All numbers are drafts to tune after a playtest; each is a config key.
- `npc_pack_chance` 0.35 per player per full-moon night (about one night in three), `npc_lone_wolf_chance` 0.5, `npc_pack_alpha_chance` 0.7 (a pack without an Alpha has no spark to give), 3 to 5 wolves (the Alpha leads only as many as the Werewolves mod lets it), `npc_max_packs_per_night` 3 per dimension so a big server is not buried. Spawn 24 to 48 blocks away, in the dark, no village within 2 chunks.
- `blood_moon_pack_chance_factor` 0.5 (the owner: half), celestials config.
- Feral: attack x1.2, speed x1.1 on a dark full-moon night: small, the wolves are already strong.
- Weak spark: 3 online days to grow, at most 2 held, 3 game days between two: slower than a steal and bounded, so the forest Alphas are not farmed.
- Surge: 24 blocks, Regeneration II 20 s, Absorption II 30 s, Strength I and Speed I 45 s; the caster then Weakness I and Slowness I for 30 s; cooldown 240 s. Howling Werewolf's numbers are much larger; ours is the light pack version.
- Spirits: 3 for 180 s, cooldown 90 s (Howling: 5 for 240 s, 60 s).
- Brother (plan 15d): 2 per werewolf and 6 per pack (an Alpha with a full pack of Betas is at most 6 wolves, about a Beta each), weight 0.5 toward the pack bonus and the minimum of 3, their part of the sum capped at `npc_bonus_share` 0.4 of the 25% cap (2 steps), so wolves alone never give more than 10% and a lone Alpha with two Brothers gives nothing. Hunt every 10 minutes for 3 animals: a chest of food for a night's waiting, not a farm.
- Tensura member: weight 0.25, 6 per pack, one step down only (a subordinate of a subordinate never joins), because a Tensura player can have dozens.

## Plan 18a: bottle my blood (R211)

| Number | Value | Why |
|---|---|---|
| `bottle_blood_cooldown_seconds` | 60 | One bottle a minute: a vampire can supply a friend, not a stockpile. |
| `bottle_blood_health_cost` | 3 (1.5 hearts, never lethal) | Real enough to count in a fight, never a way to die. |
| bottle_blood_daily_limit | 8 | Vampirism's vampire blood bottle is an ingredient (pure blood, vampire blood oil) and a task item: a day of bottling cannot supply a whole server. |

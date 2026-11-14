# Plan 10: balance and lore pass (short design)

**Why:** the owner reviewed plan 05's open points and the Tensura wiki's species list, and settled every one. This plan applies those decisions. It adds no new systems.

**Goals:** apply exactly the decisions below, and update `SPECIES-RULES.md`, the part docs, the CHANGELOG and the `wiki/` pages for each one.
**Non-goals:** show-style turning, including the Ghoul step (plan 11). The supernatural race system and hub (plan 12). Keys, skill slots and defaults (plan 13).
**Anti-goals:**
- A balance change that silently alters a save. Existing players keep what they have; only future events follow the new rules.
- A new mixin when an event or tag will do.

## Decisions (owner, 2026-11-14)

| # | Decision | Where it lands today | Change |
|---|---|---|---|
| 1 | Vampires take holy damage at ×3, with Tensura's undead side effects | `undead_treatment_enabled` defaults to on | None in code. Docs explain plainly: on means ×3 plus Tensura's undead rules; off means ×1.5 via the bridge. |
| 2 | Apex races cost 2,000,000 EP | `ApexRace.EP_REQUIREMENT = 1_000_000` | Raise to 2,000,000. Keep the config multiplier. |
| 3 | No apex race is bloodless; bites and venom must hit them | Vampire-type apex races are in `tensura:undead`, so they inherit `tensura:no_blood`, and our bite block refuses them | Mark apex races as **bitable** with our own race tag `supernaturalbloodlines:bitable`. Every bloodless check in this mod skips races with that tag: the bite block, R103 bloodless joins, venom and feeding. They stay undead, so holy damage still applies. Builder: list every reader of `no_blood` and Tensura's own bloodless checks that touch bites, and decide each one. |
| 4 | The evolution boost speeds up **EP gain only** | `RaceInstanceMixin` multiplies Tensura's combined evolution progress by 1.25, so up to 20% of conditions can be skipped | Remove the progress multiplier. Players with a species get +25% EP gain instead, through Tensura's EP-gain event if one exists; otherwise the smallest safe hook. If the mixin is no longer needed, delete it and free the slot. |
| 5 | True Alphas keep the spark | `SparkHandler`: a werewolf player who kills an Alpha **or True Alpha** steals it. Leaving or curing an Alpha passes the spark on | Only a plain Alpha can be stolen from or inherited. Killing a True Alpha is just a death. A True Alpha who is cured, or leaves the werewolves, stays True Alpha (dormant while not a werewolf). Rank perks come back on rejoining. The pack is unchanged: it dissolves on leaving, as for any Alpha. |
| 6 | Werewolves map to Tensura's Beastfolk line (the wiki's "Lycanthrope") | Not mapped | A player whose Tensura race is exactly `tensura:human` becomes `tensura:beastfolk` on becoming a werewolf (as R20a does for vampires). Alpha → `tensura:beast_lord` and True Alpha → `tensura:spirit_beast`, only when the player is already on the Beastfolk line. `tensura:divine_beast` stays a normal Tensura evolution. The race is never lowered, and other races are never touched. Builder: confirm the line's order in the jar before coding. |
| 7 | Full-moon rites work only on full-moon **nights** | `FactionRiteFactory:180` uses `MoonRule.isFullMoon` (the whole day) | Use `MoonRule.isFullMoonNight` for Rite of the Moon, Immortality and Silver Moon. Refusal messages say "night". |
| 8 | The lunar eclipse tints the moon and the sky, not only the fog | Fog colour only | Also tint the sky colour and draw the moon **dark orange** during an eclipse, on the client. Prefer render events over a mixin. |

## Approaches

Each decision has one obvious place in the existing code, so the only real choice is how to group the work: one plan, with tasks grouped by part. The alternative, one plan per decision, would mean more branches and reviews for little gain.

## Left to the builder

- The tag name and its readers (decision 3).
- The EP-gain hook (decision 4).
- The render hook for the eclipse sky (decision 8).
- The exact refusal wording.

All of them must keep the existing test runs green and add a GameTest or unit row for every decision.

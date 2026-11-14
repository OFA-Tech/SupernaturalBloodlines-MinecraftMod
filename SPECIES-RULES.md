

## Skills (plan 12, phase 12c)

Skills bought with skill points tune the numbers above through `LadderQuery.value`; every other number is unchanged. A skill counts only while the tree of the player's race lists it. The numbers a skill gives are in `data/supernaturalbloodlines/supernatural/skill/<id>.json` (`params`).

- **Silver Tongue (S-VAM-3):** compulsion needs eye contact within 6 blocks, not 4. The villager discount is 100 reputation without the skill (20 `MAJOR_POSITIVE`); with it the compulsion adds `MINOR_POSITIVE` on top, which a villager caps at 25, so the discount is worth 125 (the planned 30% could not be reached: a villager keeps at most 20 of the major kind and 25 of the minor kind).
- **Ashwarden (S-WIT-3):** no effect. A mountain ash line can already not be broken by a vampire or werewolf type, whoever placed it (`supernaturalCannotBreakLine`), so there is no number to change. Owner to patch.
- **Blood (S-VAM-1, Keen Fangs), Predator's Thirst (S-VAM-6)** and the rest of the BLOOD skills are in `docs/parts/supernatural.md`.

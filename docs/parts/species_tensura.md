# Part: species_tensura

Requires: `vampirism`, `enchanted`, `tensura`, and the `species` part active. Package: `net.ofatech.supernaturalbloodlines.parts.speciestensura`. Plan: `docs/superpowers/plans/05-species-tensura.md`.

Checklist (from the design, sections 5 and 10). Tick items as they ship.

- [ ] Tensura races are never replaced: species are a layer on top of the player's race (SPECIES-RULES R3, R20, R100).
- [ ] Species layer grants Tensura skills, attributes and an EP bonus; counts as undead where it matters.
- [ ] One apex evolution per species, unlocked for human-like races in Tensura's evolution screen.
- [ ] Intrinsic skills from Tensura's existing ones (e.g. `blood_mist`, `drain`, `beast_transformation`, `self_regeneration`, `godwolf_sense`, `charm`); final lists chosen by the builder.
- [ ] Sync both ways with the unified vampire; the species layer wins over the plain vampire mapping.
- [ ] Sireline link: `IExistence.setPermanentOwner(sireUUID)` when a sire is recorded; removed when the sire bond breaks.
- [ ] A sire can name their sired through Tensura's naming (R101).
- [ ] Rank skills for werewolf packs (ALPHA → `beast_transformation`, `godwolf_sense`; TRUE_ALPHA adds the commander skill; born lineage → `godwolf_sense` at level 5); only skills this mod granted are removed.

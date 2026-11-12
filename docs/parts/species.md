# Part: species

Requires: `vampirism`, `enchanted`. Hybrid, Original hybrid and tribrid also need `werewolves`. Package: `net.ofatech.supernaturalbloodlines.parts.species`. Plans: `docs/superpowers/plans/04-species.md`, `07-hunters.md`, `08-sirelines.md`, `09-werewolf-packs.md`.

Rules live in `core/bloodline` (`BloodlineRules`, `PackRules`) and `docs/superpowers/specs/SPECIES-RULES.md` is the source of truth. Checklist from the design (sections 4, 7, 8, 9). Tick items as they ship.

**Species (build order)**
- [ ] Originals and Original's blood (Rite of Immortality, white-oak rule, Original's blood item).
- [ ] Hybrid (doppelgänger blood during the transition, sire bond).
- [ ] Original hybrid (Rite of the Moon, needs the gene).
- [ ] Witch and siphoner (Rite of Ancestral Calling; siphoning).
- [ ] Heretic (a siphoner who joins the vampire faction).
- [ ] Tribrid (a witch who completes the hybrid transition).
- [ ] Eligibility: only human-like races; others are refused with a message.
- [ ] Death and respawn: species, sire and Original status persist; `siphoned_power` resets.
- [ ] Originals and white oak: non-oak lethal damage can't kill; white oak items and tree.
- [ ] Vampire traits for hybrid types (attributes by age, blood thirst, bite that poisons vampires).

**Hunters**
- [ ] Rite of the Five, `FiveRoster` (SavedData), `max_five`.
- [ ] The mark: perks from `markKills`, glow of vampires within 24 blocks at 5 kills, compulsion immunity.
- [ ] Losing the mark and choosing a potential.
- [ ] The hunter's curse (hallucinations) and when it lifts.
- [ ] The hunter instinct (Weakness after 3 days without a kill).

**Sirelines**
- [ ] `sire` recorded for turned players, converted creatures and hybrids; `SirelineIndex` (SavedData).
- [ ] `/bloodline tree [player]` and `/bloodline call`.
- [ ] Compulsion on villagers and lesser vampire mobs.
- [ ] Sire bond; optional `sireline_death` (default off).

**Werewolf bloodlines and packs (needs Werewolves)**
- [ ] Lineage BORN / BITTEN; hidden gene rolled on first login (`latent_gene_chance`); Rite of Lineage.
- [ ] Ranks OMEGA, BETA, ALPHA, TRUE_ALPHA and eye colours.
- [ ] Alpha spark: stolen, inherited, earned (True Alpha), given up (healing).
- [ ] Turning people: Alphas only; `alpha_bite_death_chance`.
- [ ] Packs: `PackIndex` (SavedData), `/pack invite|accept|leave|emissary|anchor`, pack bonus, Alpha Roar.
- [ ] Anchors for full-moon control.
- [ ] Weaknesses: mountain ash, lunar eclipse, electricity (Shocked, Taser).
- [ ] Tensura skills by rank, only in `species_tensura`.

**Debug**
- [x] `/supernaturalbloodlines debug get|set|event` (foundation).

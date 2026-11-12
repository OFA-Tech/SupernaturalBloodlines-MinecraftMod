# Part: artifacts

Requires: `vampirism`, `enchanted`. Werewolves and Curios are optional (the Moon Ring needs Werewolves). Package: `net.ofatech.supernaturalbloodlines.parts.artifacts`. Plan: `docs/superpowers/plans/06-artifacts.md`.

Checklist (from the design, section 6). Tick items as they ship.

- [ ] `ArtifactSlots.isWorn(player, item)`: the one helper for "is this ring equipped" (Curios `ring` slot when Curios is loaded, else the inventory).
- [ ] Sun Ring (rite *Ward of Daylight*): no sun damage while worn; bound to one vampire by UUID in a data component.
- [ ] Moon Ring (rite *Ward of the Moon*, Werewolves): no forced full-moon transformation.
- [ ] Resurrection Ring (rite *Ward of Return*): cancels death from a supernatural attacker, 3 charges.
- [ ] Vervain: crop and tea; burns vampires and hybrids that bite or hit barehanded; compulsion immunity; harms vampires who eat it.
- [ ] White Oak Ash Dagger: *Desiccated* on an Original until the dagger is pulled; heavy damage to a supernatural wielder.
- [ ] The Cure (rite *Rite of the Cure*): removes every layer and leaves the faction; sets `BloodlineEvent.DRANK_THE_CURE`.
- [ ] Vampire Compass: points to the nearest vampire, hybrid or Original within 64 blocks.
- [ ] Hearing Device: stuns supernaturals within 5 blocks for 5 s; 60 s cooldown.
- [ ] Mountain ash (with packs): dust line, ash tree, blocks supernatural entities.
- [ ] Taser (with packs): *Shocked* effect for werewolves.

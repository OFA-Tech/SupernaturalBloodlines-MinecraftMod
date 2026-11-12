# Part: vampirism_tensura

Requires: `vampirism`, `tensura`. Werewolves is optional. Package: `net.ofatech.supernaturalbloodlines.parts.vampirismtensura`. Plan: `docs/superpowers/plans/01-vampirism-tensura.md`.

Checklist (from the design, section 3). Tick items as they ship.

**Unified vampire**
- [ ] Vampirism → Tensura: joining the vampire faction (sanguinare finished) sets the Tensura race to the configured `join_race` (default `tensura:vampire`).
- [ ] Tensura → Vampirism: a Tensura race in the vampire line joins the faction at the mapped level (`ghoul` no faction; `vampire` level 1 or the current level if higher; `vampire_overcomer` and `vampire_lord` at least level 7; `divine_vampire` max level).
- [ ] Leaving the faction (cure) sets the race to `cure_race` (default `tensura:human`).
- [ ] Sun burn: Tensura's own vampire burn is disabled for faction vampires (a Tensura config option if one exists, otherwise one mixin on `VampireRace.shouldBurn`, listed in `docs/mixins.md`).

**Hunters**
- [ ] `CanJoinFaction` returns DENY for hunters whose race has the tags `tensura:undead` or `daemon`.
- [ ] A hunter whose race becomes undead or daemon leaves the hunter faction, with a chat message.

**Werewolves (when installed)**
- [ ] Joining is denied for the race tags `tensura:undead`, `daemon` and `no_blood`.
- [ ] Werewolf levels grant configured Tensura skills (default `tensura:godwolf_sense` at max level).

**Other**
- [ ] Holy damage both ways: check that `vampirism:holy_water` counts as Tensura holy damage; make Tensura holy damage hurt Vampirism vampires as holy water does.
- [ ] Bloodless races can't be bitten: cancel the bite for the tag `tensura:no_blood`, and blood 0 for Tensura bloodless mobs (data map).
- [ ] EP for Vampirism and Werewolves mobs (data `entity_existence`).

# Part: enchanted_tensura

Requires: `enchanted`, `tensura`. Package: `net.ofatech.supernaturalbloodlines.parts.enchantedtensura`. Plan: `docs/superpowers/plans/02-enchanted-tensura.md`.

Checklist (from the design, section 1). Tick items as they ship.

- [ ] Altar power from Tensura magic blocks (data): magic ore, crystal and magisteel block ids, listed from the Tensura jar.
- [ ] Curse *Magicule Drain*: slowly lowers magicules while active.
- [ ] Curse *Skill Seal*: one random learned skill can't be activated while active (`SkillEvents.ACTIVATE_SKILL` interrupt).
- [ ] Rite of Purification (data): holy damage in an area to Tensura-undead races and `minecraft:undead`; the damage type id contains "holy".
- [ ] Rite *Magicule Offering*: converts the caster's magicules into rite power (custom factory, or a PowerProvider shim).
- [ ] Spirit Poppet (code): protects against damage in `tensura:is_spiritual`.
- [ ] EP for Enchanted mobs (data): `entity_existence` entries.

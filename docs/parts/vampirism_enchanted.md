# Part: vampirism_enchanted

Requires: `vampirism`, `enchanted`. Werewolves is optional. Package: `net.ofatech.supernaturalbloodlines.parts.vampirismenchanted`. Plan: `docs/superpowers/plans/03-vampirism-enchanted.md`.

Checklist (from the design, section 2). Tick items as they ship.

- [ ] Plant parity (data): Enchanted garlic is accepted where Vampirism garlic is (tags, `c:crops/garlic`).
- [ ] Plant parity (data): Enchanted wolfsbane counts as Werewolves' wolfsbane (tags and recipe ingredient).
- [ ] Altar power from Vampirism and Werewolves plants and blocks.
- [ ] Rite of Cleansing Blood: cures vampirism by leaving the faction.
- [ ] Rite of the Silver Moon: cures lycanthropy; only when Werewolves is installed.
- [ ] Blood Offering: uses Vampirism blood bottles as power.
- [ ] A custom rite factory that calls `IFactionPlayerHandler` (used by all rites above).
- [ ] Silver Poppet: protects against silver and werewolf bites, when Werewolves is installed.
- [ ] Sun Poppet: absorbs one sun-damage tick burst for vampires.
- [ ] Blood values for Enchanted mobs (data map).
